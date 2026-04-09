import { type JSX, useMemo, useState } from "react";

import {
  AutoEmbedOption,
  type EmbedConfig,
  type EmbedMatchResult,
  LexicalAutoEmbedPlugin,
  URL_MATCHER,
} from "@lexical/react/LexicalAutoEmbedPlugin";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import type { LexicalEditor } from "lexical";

import { Popover as PopoverPrimitive } from "radix-ui";

// import { TwitterIcon, YoutubeIcon } from "lucide-react"

import { useEditorModal } from "@/components/editor/use-modal";
import { INSERT_TWEET_COMMAND } from "@/components/editor/twitter-plugin";
import { INSERT_YOUTUBE_COMMAND } from "@/components/editor/youtube-plugin";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

const YoutubeIcon = (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className="lucide lucide-youtube"
  >
    <path d="M18.5 3A3.5 3.5 0 0 0 15 6.5V17.5a3.5 3.5 0 0 0 3.5 3.5 3.5 3.5 0 0 0 3.5-3.5V6.5A3.5 3.5 0 0 0 18.5 3Z" />
    <path d="M9 12l6 3-6-3Z" />
  </svg>
);

const TwitterIcon = (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className="lucide lucide-twitter"
  >
    <path d="M22 4s-.7 2.1-2.1 3.9c-.2.2-.4.3-.7.5-.2-.2-.4-.3-.7-.5C18.1 6.1 17 5.7 16 5.7c-3 0-5.3 2.3-5.3 5.3s2.3 5.3 5.3 5.3c1 0 1.8-.3 2.4-.7-.8-.2-1.4-.6-1.7-1.2h-.1c-.5 1-1.4 1.7-2.5 1.7-1.2 0-2.3-.6-2.9-1.5.5.1 1 .1 1.4.1 2.6 0 4.7-2.1 4.7-4.7s-2.1-4.7-4.7-4.7c-.8 0-1.5.2-2.1.5.7.8 1.7 1.3 2.8 1.3" />
  </svg>
);

export interface CustomEmbedConfig extends EmbedConfig {
  // Human readable name of the embeded content e.g. Tweet or Google Map.
  contentName: string;

  // Icon for display.
  icon?: JSX.Element;

  // An example of a matching url https://twitter.com/jack/status/20
  exampleUrl: string;

  // For extra searching.
  keywords: Array<string>;

  // Embed a Project.
  description?: string;
}

export const YoutubeEmbedConfig: CustomEmbedConfig = {
  contentName: "Youtube Video",

  exampleUrl: "https://www.youtube.com/watch?v=jNQXAC9IVRw",

  // Icon for display.
  icon: YoutubeIcon,

  insertNode: (editor: LexicalEditor, result: EmbedMatchResult) => {
    editor.dispatchCommand(INSERT_YOUTUBE_COMMAND, result.id);
  },

  keywords: ["youtube", "video"],

  // Determine if a given URL is a match and return url data.
  parseUrl: async (url: string) => {
    const match =
      /^.*(youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/.exec(url);

    const id = match ? (match?.[2].length === 11 ? match[2] : null) : null;

    if (id != null) {
      return {
        id,
        url,
      };
    }

    return null;
  },

  type: "youtube-video",
};

export const TwitterEmbedConfig: CustomEmbedConfig = {
  // e.g. Tweet or Google Map.
  contentName: "Tweet",

  exampleUrl: "https://twitter.com/jack/status/20",

  // Icon for display.
  icon: TwitterIcon,

  // Create the Lexical embed node from the url data.
  insertNode: (editor: LexicalEditor, result: EmbedMatchResult) => {
    editor.dispatchCommand(INSERT_TWEET_COMMAND, result.id);
  },

  // For extra searching.
  keywords: ["tweet", "twitter"],

  // Determine if a given URL is a match and return url data.
  parseUrl: (text: string) => {
    const match =
      /^https:\/\/(twitter|x)\.com\/(#!\/)?(\w+)\/status(es)*\/(\d+)/.exec(
        text,
      );

    if (match != null) {
      return {
        id: match[5],
        url: match[1],
      };
    }

    return null;
  },

  type: "tweet",
};

export const EmbedConfigs = [TwitterEmbedConfig, YoutubeEmbedConfig];

const debounce = (callback: (text: string) => void, delay: number) => {
  let timeoutId: number;
  return (text: string) => {
    window.clearTimeout(timeoutId);
    timeoutId = window.setTimeout(() => {
      callback(text);
    }, delay);
  };
};

export function AutoEmbedDialog({
  embedConfig,
  onClose,
}: {
  embedConfig: CustomEmbedConfig;
  onClose: () => void;
}): JSX.Element {
  const [text, setText] = useState("");
  const [editor] = useLexicalComposerContext();
  const [embedResult, setEmbedResult] = useState<EmbedMatchResult | null>(null);

  const validateText = useMemo(
    () =>
      debounce((inputText: string) => {
        const urlMatch = URL_MATCHER.exec(inputText);
        if (embedConfig != null && inputText != null && urlMatch != null) {
          Promise.resolve(embedConfig.parseUrl(inputText)).then(
            (parseResult) => {
              setEmbedResult(parseResult);
            },
          );
        } else if (embedResult != null) {
          setEmbedResult(null);
        }
      }, 200),
    [embedConfig, embedResult],
  );

  const onClick = () => {
    if (embedResult != null) {
      embedConfig.insertNode(editor, embedResult);
      onClose();
    }
  };

  return (
    <div className="">
      <div className="space-y-4">
        <Input
          type="text"
          placeholder={embedConfig.exampleUrl}
          value={text}
          data-test-id={`${embedConfig.type}-embed-modal-url`}
          onChange={(e) => {
            const { value } = e.target;
            setText(value);
            validateText(value);
          }}
        />
        <DialogFooter>
          <Button
            disabled={!embedResult}
            onClick={onClick}
            data-test-id={`${embedConfig.type}-embed-modal-submit-btn`}
          >
            Embed
          </Button>
        </DialogFooter>
      </div>
    </div>
  );
}

export function AutoEmbedPlugin(): JSX.Element {
  const [modal, showModal] = useEditorModal();

  const openEmbedModal = (embedConfig: CustomEmbedConfig) => {
    showModal(`Embed ${embedConfig.contentName}`, (onClose) => (
      <AutoEmbedDialog embedConfig={embedConfig} onClose={onClose} />
    ));
  };

  const getMenuOptions = (
    activeEmbedConfig: CustomEmbedConfig,
    embedFn: () => void,
    dismissFn: () => void,
  ) => {
    return [
      new AutoEmbedOption("Dismiss", {
        onSelect: dismissFn,
      }),
      new AutoEmbedOption(`Embed ${activeEmbedConfig.contentName}`, {
        onSelect: embedFn,
      }),
    ];
  };

  return (
    <>
      {modal}
      <LexicalAutoEmbedPlugin<CustomEmbedConfig>
        embedConfigs={EmbedConfigs}
        onOpenEmbedModalForConfig={openEmbedModal}
        getMenuOptions={getMenuOptions}
        menuRenderFn={(
          anchorElementRef,
          { options, selectOptionAndCleanUp },
        ) => {
          return anchorElementRef.current ? (
            <Popover open={true}>
              <PopoverPrimitive.Portal container={anchorElementRef.current}>
                <div className="-translate-y-full transform">
                  <PopoverTrigger />
                  <PopoverContent
                    className="min-w-36 p-0"
                    align="start"
                    side="right"
                  >
                    <Command>
                      <CommandList>
                        <CommandGroup>
                          {options.map((option) => (
                            <CommandItem
                              key={option.key}
                              value={option.title}
                              onSelect={() => {
                                selectOptionAndCleanUp(option);
                              }}
                              className="flex items-center gap-2"
                            >
                              {option.title}
                            </CommandItem>
                          ))}
                        </CommandGroup>
                      </CommandList>
                    </Command>
                  </PopoverContent>
                </div>
              </PopoverPrimitive.Portal>
            </Popover>
          ) : null;
        }}
      />
    </>
  );
}
