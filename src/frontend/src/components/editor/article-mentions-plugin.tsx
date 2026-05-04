import { type JSX, useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import {
  MenuOption,
  type MenuTextMatch,
  useBasicTypeaheadTriggerMatch,
} from "@lexical/react/LexicalTypeaheadMenuPlugin";
import { LexicalTypeaheadMenuPlugin } from "@lexical/react/LexicalTypeaheadMenuPlugin";
import { TextNode } from "lexical";
import { BookOpenIcon } from "lucide-react";
import { $createArticleMentionNode } from "@/components/editor/article-mention-node";
import {
  Command,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { toQueryString } from "@/utils/formatting";

const TRIGGERS = ["\\$"].join("");
const VALID_CHARS = "[^\\s\\$]";
const LENGTH_LIMIT = 75;

const ArticleMentionsRegex = new RegExp(
  "(^|\\s|\\()(" +
    "[" +
    TRIGGERS +
    "]" +
    "(" +
    VALID_CHARS +
    "{0," +
    LENGTH_LIMIT +
    "}" +
    ")" +
    ")$",
);

const SUGGESTION_LIST_LENGTH_LIMIT = 6;

interface ArticleSuggestion {
  id: number;
  name: string;
  title: string;
  detailPath: string;
}

interface ArticleSuggestionResponse {
  items?: ArticleSuggestion[];
}

const articleSuggestCache = new Map<string, ArticleSuggestion[] | null>();

function useArticleSuggestService(
  queryString: string | null,
): ArticleSuggestion[] {
  const [results, setResults] = useState<ArticleSuggestion[]>([]);
  const active = queryString != null && queryString.length >= 1;

  useEffect(() => {
    if (!active) {
      return;
    }

    const apiBase = "/api/articles";
    const cacheKey = `${apiBase}:${queryString}`;
    const cached = articleSuggestCache.get(cacheKey);

    if (cached === null) {
      return;
    }

    articleSuggestCache.set(cacheKey, null);

    const queryParams: Record<string, string> = { q: queryString! };
    const url = `${apiBase}/suggest${toQueryString(queryParams)}`;
    fetch(url, { credentials: "same-origin", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) {
          return { items: [] } as ArticleSuggestionResponse;
        }
        return (await response.json()) as ArticleSuggestionResponse;
      })
      .then((data) => {
        const items = data.items || [];
        articleSuggestCache.set(cacheKey, items);
        setResults(items);
      })
      .catch(() => {
        articleSuggestCache.set(cacheKey, []);
        setResults([]);
      });
  }, [active, queryString]);

  if (queryString == null) {
    return [];
  }

  const apiBase = "/api/articles";
  const cacheKey = `${apiBase}:${queryString}`;
  const cached = articleSuggestCache.get(cacheKey);
  return cached ?? results;
}

function checkForArticleMention(
  text: string,
  minMatchLength: number,
): MenuTextMatch | null {
  const match = ArticleMentionsRegex.exec(text);
  if (match !== null) {
    const maybeLeadingWhitespace = match[1];
    const matchingString = match[3];
    if (matchingString.length >= minMatchLength) {
      return {
        leadOffset: match.index + maybeLeadingWhitespace.length,
        matchingString,
        replaceableString: match[2],
      };
    }
  }
  return null;
}

function getPossibleArticleQueryMatch(text: string): MenuTextMatch | null {
  return checkForArticleMention(text, 1);
}

class ArticleTypeaheadOption extends MenuOption {
  id: number;
  articleName: string;
  title: string;

  constructor(id: number, articleName: string, title: string) {
    super(`${id}-${articleName}`);
    this.id = id;
    this.articleName = articleName;
    this.title = title;
  }
}

export function ArticleMentionsPlugin(): JSX.Element | null {
  const [editor] = useLexicalComposerContext();

  const [queryString, setQueryString] = useState<string | null>(null);

  const results = useArticleSuggestService(queryString);

  const checkForSlashTriggerMatch = useBasicTypeaheadTriggerMatch("/", {
    minLength: 0,
  });

  const options = useMemo(
    () =>
      results
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(
          (result) =>
            new ArticleTypeaheadOption(result.id, result.name, result.title),
        )
        .slice(0, SUGGESTION_LIST_LENGTH_LIMIT),
    [results],
  );

  const onSelectOption = useCallback(
    (
      selectedOption: ArticleTypeaheadOption,
      nodeToReplace: TextNode | null,
      closeMenu: () => void,
    ) => {
      editor.update(() => {
        const articleMentionNode = $createArticleMentionNode(
          selectedOption.id,
          selectedOption.articleName,
          selectedOption.title,
        );
        if (nodeToReplace) {
          nodeToReplace.replace(articleMentionNode);
        }
        articleMentionNode.select();
        closeMenu();
      });
    },
    [editor],
  );

  const checkForMentionMatch = useCallback(
    (text: string) => {
      const slashMatch = checkForSlashTriggerMatch(text, editor);
      if (slashMatch !== null) {
        return null;
      }
      return getPossibleArticleQueryMatch(text);
    },
    [checkForSlashTriggerMatch, editor],
  );

  return (
    <LexicalTypeaheadMenuPlugin
      onQueryChange={setQueryString}
      onSelectOption={onSelectOption}
      triggerFn={checkForMentionMatch}
      options={options}
      menuRenderFn={(
        anchorElementRef,
        { selectedIndex, selectOptionAndCleanUp, setHighlightedIndex },
      ) => {
        return anchorElementRef.current && results.length
          ? createPortal(
              <div className="absolute z-10 min-w-64 rounded-md shadow-md">
                <Command
                  onKeyDown={(e) => {
                    if (e.key === "ArrowUp") {
                      e.preventDefault();
                      setHighlightedIndex(
                        selectedIndex !== null
                          ? (selectedIndex - 1 + options.length) %
                              options.length
                          : options.length - 1,
                      );
                    } else if (e.key === "ArrowDown") {
                      e.preventDefault();
                      setHighlightedIndex(
                        selectedIndex !== null
                          ? (selectedIndex + 1) % options.length
                          : 0,
                      );
                    }
                  }}
                >
                  <CommandList>
                    <CommandGroup>
                      {options.map((option: ArticleTypeaheadOption) => (
                        <CommandItem
                          key={option.key}
                          value={`${option.id} ${option.articleName} ${option.title}`}
                          onSelect={() => {
                            selectOptionAndCleanUp(option);
                          }}
                        >
                          <span className="shrink-0 text-red-500">
                            <BookOpenIcon className="size-4" />
                          </span>
                          <span className="truncate font-medium text-sm">
                            {option.title}
                          </span>
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  </CommandList>
                </Command>
              </div>,
              anchorElementRef.current,
            )
          : null;
      }}
    />
  );
}
