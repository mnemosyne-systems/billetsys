/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent, RefObject } from "react";
import { useRef } from "react";
import MarkdownContent from "./MarkdownContent";

type MarkdownAction =
  | "bold"
  | "italic"
  | "heading"
  | "list"
  | "quote"
  | "code"
  | "code-block"
  | "link"
  | "media";

const MARKDOWN_CODE_BLOCK_LANGUAGES: Array<[string, string]> = [
  ["", "Text"],
  ["bash", "Bash"],
  ["c", "C"],
  ["cpp", "C++"],
  ["go", "Go"],
  ["html", "HTML"],
  ["java", "Java"],
  ["javascript", "JS"],
  ["json", "JSON"],
  ["python", "Py"],
  ["rust", "Rust"],
  ["sql", "SQL"],
  ["xml", "XML"],
];

function markdownActionText(
  action: MarkdownAction,
  selectedText: string,
  option = "",
): string | null {
  const text = selectedText || "text";
  if (action === "bold") {
    return `**${text}**`;
  }
  if (action === "italic") {
    return `*${text}*`;
  }
  if (action === "heading") {
    return `## ${selectedText || "Heading"}`;
  }
  if (action === "list") {
    return selectedText
      ? selectedText
          .split("\n")
          .map((line) => `- ${line}`)
          .join("\n")
      : "- Item";
  }
  if (action === "quote") {
    return selectedText
      ? selectedText
          .split("\n")
          .map((line) => `> ${line}`)
          .join("\n")
      : "> Quote";
  }
  if (action === "code") {
    return `\`${text}\``;
  }
  if (action === "code-block") {
    return `\`\`\`${option || ""}\n${selectedText || "code"}\n\`\`\``;
  }
  if (action === "link") {
    const href = window.prompt("Link URL", "https://");
    if (!href) {
      return null;
    }
    return `[${selectedText || "link"}](${href})`;
  }
  if (action === "media") {
    const href = window.prompt("Media URL", "https://");
    if (!href) {
      return null;
    }
    return `![${selectedText || "media"}](${href})`;
  }
  return selectedText;
}

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  inputRef?: RefObject<HTMLTextAreaElement | null>;
  rows?: number;
  name?: string;
  required?: boolean;
}

export default function MarkdownEditor({
  value,
  onChange,
  inputRef,
  rows = 10,
  name,
  required = false,
}: MarkdownEditorProps) {
  const fallbackInputRef = useRef<HTMLTextAreaElement | null>(null);
  const textareaRef = inputRef || fallbackInputRef;

  const applyAction = (action: MarkdownAction, option = "") => {
    if (!textareaRef.current) {
      return;
    }
    const textarea = textareaRef.current;
    const selectionStart = textarea.selectionStart ?? value.length;
    const selectionEnd = textarea.selectionEnd ?? value.length;
    const selectedText = value.slice(selectionStart, selectionEnd);
    const replacement = markdownActionText(action, selectedText, option);
    if (replacement == null) {
      return;
    }
    const nextValue = `${value.slice(0, selectionStart)}${replacement}${value.slice(selectionEnd)}`;
    onChange(nextValue);
    requestAnimationFrame(() => {
      textarea.focus();
      const cursor = selectionStart + replacement.length;
      textarea.setSelectionRange(cursor, cursor);
    });
  };

  return (
    <div className="markdown-editor">
      <div className="markdown-toolbar">
        <button
          type="button"
          onClick={() => applyAction("bold")}
          aria-label="Bold"
        >
          <strong>B</strong>
        </button>
        <button
          type="button"
          onClick={() => applyAction("italic")}
          aria-label="Italic"
        >
          <em>I</em>
        </button>
        <button
          type="button"
          onClick={() => applyAction("heading")}
          aria-label="Heading"
        >
          H
        </button>
        <button
          type="button"
          onClick={() => applyAction("list")}
          aria-label="List"
        >
          •
        </button>
        <button
          type="button"
          onClick={() => applyAction("quote")}
          aria-label="Quote"
        >
          &quot;
        </button>
        <button
          type="button"
          onClick={() => applyAction("code")}
          aria-label="Code"
        >
          {"</>"}
        </button>
        <select
          className="markdown-toolbar-select"
          defaultValue="__label"
          onChange={(event: ChangeEvent<HTMLSelectElement>) => {
            const selectedLanguage = event.target.value;
            if (selectedLanguage === "__label") {
              return;
            }
            applyAction("code-block", selectedLanguage);
            event.target.value = "__label";
          }}
          aria-label="Code block language"
        >
          <option value="__label" hidden>
            Code
          </option>
          {MARKDOWN_CODE_BLOCK_LANGUAGES.map(([code, label]) => (
            <option key={code || "plaintext"} value={code}>
              {label}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={() => applyAction("link")}
          aria-label="Link"
        >
          Link
        </button>
        <button
          type="button"
          onClick={() => applyAction("media")}
          aria-label="Media"
        >
          Img
        </button>
      </div>
      <div className="markdown-panels">
        <div className="markdown-panel">
          <div className="markdown-panel-header">Write</div>
          <textarea
            ref={textareaRef}
            name={name}
            rows={rows}
            value={value}
            onChange={(event: ChangeEvent<HTMLTextAreaElement>) =>
              onChange(event.target.value)
            }
            required={required}
          />
        </div>
        <div className="markdown-panel">
          <div className="markdown-panel-header">Preview</div>
          <div className="markdown-preview markdown-output">
            <MarkdownContent>{value || ""}</MarkdownContent>
          </div>
        </div>
      </div>
    </div>
  );
}
