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

import { TicketIcon } from "lucide-react";

import { $createTicketMentionNode } from "@/components/editor/ticket-mention-node";
import {
  Command,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { toQueryString } from "@/utils/formatting";

const TRIGGERS = ["#"].join("");
const VALID_CHARS = "[^\\s#]";
const LENGTH_LIMIT = 75;

const TicketMentionsRegex = new RegExp(
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

interface TicketSuggestion {
  id: number;
  name: string;
  title: string;
  detailPath: string;
}

interface TicketSuggestionResponse {
  items?: TicketSuggestion[];
}

const ticketSuggestCache = new Map<string, TicketSuggestion[] | null>();

function useTicketSuggestService(
  queryString: string | null,
  apiBase: string,
): TicketSuggestion[] {
  const [results, setResults] = useState<TicketSuggestion[]>([]);
  const active = queryString != null && queryString.length >= 1;

  useEffect(() => {
    if (!active) {
      return;
    }

    const cacheKey = `${apiBase}:${queryString}`;
    const cached = ticketSuggestCache.get(cacheKey);

    if (cached === null) {
      return;
    }

    ticketSuggestCache.set(cacheKey, null);

    const url = `${apiBase}/suggest${toQueryString({ q: queryString! })}`;
    fetch(url, { credentials: "same-origin", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) {
          return { items: [] } as TicketSuggestionResponse;
        }
        return (await response.json()) as TicketSuggestionResponse;
      })
      .then((data) => {
        const items = data.items || [];
        ticketSuggestCache.set(cacheKey, items);
        setResults(items);
      })
      .catch(() => {
        ticketSuggestCache.set(cacheKey, []);
        setResults([]);
      });
  }, [active, queryString, apiBase]);

  if (queryString == null) {
    return [];
  }

  const cacheKey = `${apiBase}:${queryString}`;
  const cached = ticketSuggestCache.get(cacheKey);
  return cached ?? results;
}

function checkForTicketMention(
  text: string,
  minMatchLength: number,
): MenuTextMatch | null {
  const match = TicketMentionsRegex.exec(text);
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

function getPossibleTicketQueryMatch(text: string): MenuTextMatch | null {
  return checkForTicketMention(text, 1);
}

class TicketTypeaheadOption extends MenuOption {
  id: number;
  ticketName: string;
  title: string;

  constructor(id: number, ticketName: string, title: string) {
    super(`${id}-${ticketName}`);
    this.id = id;
    this.ticketName = ticketName;
    this.title = title;
  }
}

interface TicketMentionsPluginProps {
  apiBase: string;
}

export function TicketMentionsPlugin({
  apiBase,
}: TicketMentionsPluginProps): JSX.Element | null {
  const [editor] = useLexicalComposerContext();

  const [queryString, setQueryString] = useState<string | null>(null);

  const results = useTicketSuggestService(queryString, apiBase);

  const checkForSlashTriggerMatch = useBasicTypeaheadTriggerMatch("/", {
    minLength: 0,
  });

  const options = useMemo(
    () =>
      results
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(
          (result) =>
            new TicketTypeaheadOption(result.id, result.name, result.title),
        )
        .slice(0, SUGGESTION_LIST_LENGTH_LIMIT),
    [results],
  );

  const onSelectOption = useCallback(
    (
      selectedOption: TicketTypeaheadOption,
      nodeToReplace: TextNode | null,
      closeMenu: () => void,
    ) => {
      editor.update(() => {
        const ticketMentionNode = $createTicketMentionNode(
          selectedOption.id,
          selectedOption.ticketName,
          selectedOption.title,
        );
        if (nodeToReplace) {
          nodeToReplace.replace(ticketMentionNode);
        }
        ticketMentionNode.select();
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
      return getPossibleTicketQueryMatch(text);
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
                      {options.map((option: TicketTypeaheadOption) => (
                        <CommandItem
                          key={option.key}
                          value={`${option.id} ${option.ticketName} ${option.title}`}
                          onSelect={() => {
                            selectOptionAndCleanUp(option);
                          }}
                        >
                          <span className="shrink-0 text-red-500">
                            <TicketIcon className="size-4" />
                          </span>
                          <span className="font-medium shrink-0 whitespace-nowrap">
                            {option.ticketName}
                          </span>
                          <span className="truncate text-muted-foreground text-sm">
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
