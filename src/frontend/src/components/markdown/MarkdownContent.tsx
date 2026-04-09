/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";

interface MarkdownContentProps {
  children?: ReactNode;
}

export default function MarkdownContent({ children }: MarkdownContentProps) {
  let content = typeof children === "string" ? children : "";

  // Auto-correct common markdown formatting mistakes (e.g. spaces inside asterisks `** bold **`)
  if (content) {
    content = content.replace(/\*\*([ \t]+)?([^*]+?)([ \t]+)?\*\*/g, "**$2**");
    content = content.replace(
      /(^|[^A-Za-z0-9_*])\*([ \t]+)?([^*]+?)([ \t]+)?\*/g,
      "$1*$3*",
    );
  }

  return (
    <ReactMarkdown rehypePlugins={[rehypeHighlight]}>{content}</ReactMarkdown>
  );
}
