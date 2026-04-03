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
  return (
    <ReactMarkdown rehypePlugins={[rehypeHighlight]}>
      {typeof children === "string" ? children : ""}
    </ReactMarkdown>
  );
}
