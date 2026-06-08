/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import {
  Fragment,
  useMemo,
  useRef,
  useState,
  useEffect,
  useCallback,
  Children,
  isValidElement,
  type ComponentPropsWithoutRef,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import ReactMarkdown from "react-markdown";
import type { Components } from "react-markdown";
import rehypeHighlight from "rehype-highlight";

import { cn } from "@/lib/utils";
import { buttonVariants } from "@/components/ui/button";
import type {
  CrossReferenceEntry,
  ArticleReferenceEntry,
} from "@/types/domain/tickets";
import { TicketHoverPreview } from "@/components/tickets/TicketHoverPreview";
import { ArticleHoverPreview } from "@/components/articles/ArticleHoverPreview";

interface MarkdownContentProps {
  children?: ReactNode;
  className?: string;
  crossReferences?: CrossReferenceEntry[];
  articleReferences?: ArticleReferenceEntry[];
}

const markdownLinkClassName =
  "font-medium text-[var(--color-header-bg)] underline underline-offset-2 hover:text-primary";

type MarkdownBlock =
  | {
      content: string;
      type: "markdown";
    }
  | {
      headers: string[];
      rows: string[][];
      type: "table";
    };

function ZoomableImage({
  src,
  alt,
  className,
  ...props
}: ComponentPropsWithoutRef<"img">) {
  const [isOpen, setIsOpen] = useState(false);
  const [isVisible, setIsVisible] = useState(false);
  const [isClosing, setIsClosing] = useState(false);

  const openZoom = useCallback(() => {
    if (!isOpen) {
      setIsOpen(true);
      setIsClosing(false);

      // Allow DOM to render before triggering transition
      requestAnimationFrame(() => {
        setIsVisible(true);
      });
    }
  }, [isOpen]);

  const closeZoom = useCallback(() => {
    if (isOpen && !isClosing) {
      setIsVisible(false);
      setIsClosing(true);

      setTimeout(() => {
        setIsOpen(false);
        setIsClosing(false);
      }, 300); // match transition duration
    }
  }, [isOpen, isClosing]);

  useEffect(() => {
    if (!isOpen || isClosing) return;

    const handleScroll = () => {
      closeZoom();
    };

    window.addEventListener("scroll", handleScroll, {
      capture: true,
      passive: true,
    });

    return () => {
      window.removeEventListener("scroll", handleScroll, { capture: true });
    };
  }, [isOpen, isClosing, closeZoom]);

  const portalContent = isOpen
    ? createPortal(
        <div
          className="fixed inset-0 z-[999999] m-0 flex h-screen w-screen items-center justify-center border-none p-0"
          onClick={closeZoom}
        >
          <div
            className={cn(
              "fixed inset-0 bg-background transition-opacity duration-300 ease-out",
              isVisible ? "opacity-100" : "opacity-0",
            )}
          />
          <div
            className={cn(
              "relative z-10 flex max-h-[70vh] max-w-[70vw] items-center justify-center transition-all duration-300 ease-out transform pointer-events-none",
              isVisible ? "opacity-100 scale-100" : "opacity-0 scale-95",
            )}
          >
            <img
              src={src}
              alt={alt}
              className="max-h-[70vh] max-w-[70vw] cursor-zoom-out object-contain rounded-md shadow-2xl ring-1 ring-border pointer-events-auto"
            />
          </div>
        </div>,
        document.body,
      )
    : null;

  return (
    <>
      <img
        src={src}
        alt={alt}
        className={cn(
          "my-4 max-w-full cursor-zoom-in rounded-md border border-border shadow-sm transition-transform hover:opacity-90",
          className,
        )}
        onClick={openZoom}
        {...props}
      />
      {portalContent}
    </>
  );
}

function MarkdownCodeBlock({
  children,
  className,
  ...props
}: ComponentPropsWithoutRef<"pre">) {
  const preRef = useRef<HTMLPreElement | null>(null);
  const [copied, setCopied] = useState(false);

  const copyCode = async () => {
    const text = preRef.current?.innerText || "";

    if (!text) {
      return;
    }

    await navigator.clipboard.writeText(text);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  };

  return (
    <div className="group relative my-4">
      <button
        type="button"
        className={cn(
          buttonVariants({ size: "sm", variant: "outline" }),
          "absolute right-2 top-2 h-auto px-2 py-1 text-xs shadow-sm",
        )}
        onClick={copyCode}
      >
        {copied ? "Copied" : "Copy"}
      </button>
      <pre
        ref={preRef}
        className={cn(
          "overflow-x-auto rounded-md border bg-muted p-4 pr-16 text-sm",
          className,
        )}
        {...props}
      >
        {children}
      </pre>
    </div>
  );
}

function buildLinkComponent(
  crossReferences?: CrossReferenceEntry[],
  articleReferences?: ArticleReferenceEntry[],
): Components["a"] {
  function MarkdownLink({
    className,
    href,
    children,
    ...props
  }: ComponentPropsWithoutRef<"a">) {
    if (crossReferences && href) {
      const match = href.match(/\/(?:support|superuser|user)\/tickets\/(\d+)$/);
      if (match) {
        const ticketId = Number(match[1]);
        const ref = crossReferences.find((r) => r.ticketId === ticketId);
        if (ref) {
          return (
            <TicketHoverPreview
              ticketName={ref.ticketName}
              ticketTitle={ref.ticketTitle}
              status={ref.status}
              companyName={ref.companyName}
              levelName={ref.levelName}
              detailPath={ref.detailPath}
              className={cn(markdownLinkClassName, className)}
            >
              {children}
            </TicketHoverPreview>
          );
        }
      }
    }
    if (articleReferences && href) {
      const match = href.match(/\/articles\/(\d+)$/);
      if (match) {
        const articleId = Number(match[1]);
        const ref = articleReferences.find((r) => r.articleId === articleId);
        if (ref) {
          return (
            <ArticleHoverPreview
              articleTitle={ref.articleTitle}
              articleExcerpt={ref.articleExcerpt}
              detailPath={ref.detailPath}
              className={cn(markdownLinkClassName, className)}
            >
              {children}
            </ArticleHoverPreview>
          );
        }
      }
    }
    return (
      <a
        className={cn(markdownLinkClassName, className)}
        href={href}
        target="_blank"
        rel="noreferrer"
        {...props}
      >
        {children}
      </a>
    );
  }
  return MarkdownLink;
}

export function generateSlug(children: ReactNode): string {
  let text = "";
  Children.forEach(children, (child) => {
    if (typeof child === "string") {
      text += child;
    } else if (isValidElement(child)) {
      const nestedChildren = (child.props as Record<string, unknown>).children;
      if (nestedChildren) {
        text += generateSlug(nestedChildren as ReactNode);
      }
    }
  });
  return text
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^\w-]+/g, "")
    .replace(/--+/g, "-")
    .replace(/^-+/, "")
    .replace(/-+$/, "");
}

const markdownComponents: Components = {
  a: buildLinkComponent(),
  blockquote: ({
    className,
    ...props
  }: ComponentPropsWithoutRef<"blockquote">) => (
    <blockquote
      className={cn(
        "my-4 border-l-4 border-border pl-4 italic text-muted-foreground",
        className,
      )}
      {...props}
    />
  ),
  code: ({ className, ...props }: ComponentPropsWithoutRef<"code">) => (
    <code
      className={cn(
        "whitespace-pre-wrap rounded bg-muted px-1 py-0.5 font-mono text-[0.9em]",
        className,
      )}
      {...props}
    />
  ),
  h1: ({ className, ...props }: ComponentPropsWithoutRef<"h1">) => (
    <h1
      className={cn(
        "mb-4 mt-6 text-3xl font-bold leading-tight text-foreground first:mt-0",
        className,
      )}
      {...props}
    />
  ),
  h2: ({ className, children, ...props }: ComponentPropsWithoutRef<"h2">) => (
    <h2
      id={generateSlug(children)}
      className={cn(
        "mb-3 mt-5 text-2xl font-semibold leading-tight text-foreground first:mt-0",
        className,
      )}
      {...props}
    >
      {children}
    </h2>
  ),
  h3: ({ className, children, ...props }: ComponentPropsWithoutRef<"h3">) => (
    <h3
      id={generateSlug(children)}
      className={cn(
        "mb-3 mt-4 text-xl font-semibold leading-snug text-foreground first:mt-0",
        className,
      )}
      {...props}
    >
      {children}
    </h3>
  ),
  hr: ({ className, ...props }: ComponentPropsWithoutRef<"hr">) => (
    <hr className={cn("my-6 border-border", className)} {...props} />
  ),
  img: ({ className, alt, ...props }: ComponentPropsWithoutRef<"img">) => (
    <ZoomableImage className={className} alt={alt} {...props} />
  ),
  li: ({ className, ...props }: ComponentPropsWithoutRef<"li">) => (
    <li className={cn("my-1 pl-1", className)} {...props} />
  ),
  ol: ({ className, ...props }: ComponentPropsWithoutRef<"ol">) => (
    <ol
      className={cn("my-3 list-decimal space-y-1 pl-6", className)}
      {...props}
    />
  ),
  p: ({ className, ...props }: ComponentPropsWithoutRef<"p">) => (
    <p
      className={cn(
        "my-3 whitespace-pre-wrap leading-7 first:mt-0 last:mb-0",
        className,
      )}
      {...props}
    />
  ),
  pre: ({ className, ...props }: ComponentPropsWithoutRef<"pre">) => (
    <MarkdownCodeBlock className={className} {...props} />
  ),
  table: ({ className, ...props }: ComponentPropsWithoutRef<"table">) => (
    <div className="my-4 overflow-x-auto">
      <table
        className={cn("w-full border-collapse text-sm", className)}
        {...props}
      />
    </div>
  ),
  td: ({ className, ...props }: ComponentPropsWithoutRef<"td">) => (
    <td className={cn("border px-3 py-2 align-top", className)} {...props} />
  ),
  th: ({ className, ...props }: ComponentPropsWithoutRef<"th">) => (
    <th
      className={cn(
        "border bg-muted px-3 py-2 text-left font-semibold",
        className,
      )}
      {...props}
    />
  ),
  ul: ({ className, ...props }: ComponentPropsWithoutRef<"ul">) => (
    <ul className={cn("my-3 list-disc space-y-1 pl-6", className)} {...props} />
  ),
};

const tableCellMarkdownComponents: Components = {
  ...markdownComponents,
  p: ({ children }) => <>{children}</>,
};

function isTableDivider(line: string): boolean {
  return /^\s*\|?(?:\s*:?-{3,}:?\s*\|)+(?:\s*:?-{3,}:?\s*)?\|?\s*$/.test(line);
}

function isTableRow(line: string): boolean {
  return line.trim().startsWith("|") && line.trim().endsWith("|");
}

function splitTableRow(line: string): string[] {
  return line
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((cell) => cell.trim());
}

function parseMarkdownBlocks(content: string): MarkdownBlock[] {
  const lines = content.split(/\r?\n/);
  const blocks: MarkdownBlock[] = [];
  const markdownBuffer: string[] = [];

  const flushMarkdown = () => {
    const markdown = markdownBuffer.join("\n").trim();
    if (markdown) {
      blocks.push({ content: markdown, type: "markdown" });
    }
    markdownBuffer.length = 0;
  };

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const divider = lines[index + 1];

    if (isTableRow(line) && divider && isTableDivider(divider)) {
      flushMarkdown();

      const headers = splitTableRow(line);
      const rows: string[][] = [];
      index += 2;

      while (index < lines.length && isTableRow(lines[index])) {
        rows.push(splitTableRow(lines[index]));
        index += 1;
      }

      blocks.push({ headers, rows, type: "table" });
      index -= 1;
    } else {
      markdownBuffer.push(line);
    }
  }

  flushMarkdown();
  return blocks;
}

function renderMarkdown(content: string, components?: Components) {
  return (
    <ReactMarkdown
      components={components ?? markdownComponents}
      rehypePlugins={[rehypeHighlight]}
    >
      {content}
    </ReactMarkdown>
  );
}

function renderTableCell(content: string) {
  return (
    <ReactMarkdown
      components={tableCellMarkdownComponents}
      rehypePlugins={[rehypeHighlight]}
    >
      {content}
    </ReactMarkdown>
  );
}

export default function MarkdownContent({
  children,
  className,
  crossReferences,
  articleReferences,
}: MarkdownContentProps) {
  let content = typeof children === "string" ? children : "";

  // Auto-correct common markdown formatting mistakes (e.g. spaces inside asterisks `** bold **`)
  if (content) {
    content = content.replace(
      /\*\*([ \t]+)?([^*\r\n]+?)([ \t]+)?\*\*/g,
      "**$2**",
    );
    content = content.replace(
      /(^|[^A-Za-z0-9_*])\*([ \t]+)?([^*\r\n]+?)([ \t]+)?\*/g,
      "$1*$3*",
    );
  }

  const blocks = parseMarkdownBlocks(content);

  const components = useMemo(() => {
    if (
      (!crossReferences || crossReferences.length === 0) &&
      (!articleReferences || articleReferences.length === 0)
    )
      return undefined;
    return {
      ...markdownComponents,
      a: buildLinkComponent(crossReferences, articleReferences),
    };
  }, [crossReferences, articleReferences]);

  return (
    <div className={cn("max-w-none text-sm text-foreground", className)}>
      {blocks.map((block, blockIndex) => {
        if (block.type === "markdown") {
          return (
            <Fragment key={`markdown-${blockIndex}`}>
              {renderMarkdown(block.content, components)}
            </Fragment>
          );
        }

        return (
          <div key={`table-${blockIndex}`} className="my-4 overflow-x-auto">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr>
                  {block.headers.map((header, headerIndex) => (
                    <th
                      key={`header-${headerIndex}`}
                      className="border bg-muted px-3 py-2 text-left font-semibold"
                    >
                      {renderTableCell(header)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {block.rows.map((row, rowIndex) => (
                  <tr key={`row-${rowIndex}`}>
                    {block.headers.map((_, cellIndex) => (
                      <td
                        key={`cell-${rowIndex}-${cellIndex}`}
                        className="border px-3 py-2 align-top"
                      >
                        {renderTableCell(row[cellIndex] || "")}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      })}
    </div>
  );
}
