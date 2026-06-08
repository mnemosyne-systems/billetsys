/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState, useMemo } from "react";
import { Menu } from "lucide-react";
import { cn } from "@/lib/utils";
import { generateSlug } from "../markdown/MarkdownContent";

interface TocEntry {
  id: string;
  text: string;
  level: number;
}

interface TableOfContentsProps {
  markdown: string;
  className?: string;
}

export default function TableOfContents({
  markdown,
  className,
}: TableOfContentsProps) {
  const [activeId, setActiveId] = useState<string>("");

  const toc = useMemo(() => {
    const headings: TocEntry[] = [];
    const lines = markdown.split("\n");
    let inCodeBlock = false;

    for (const line of lines) {
      if (line.trim().startsWith("```")) {
        inCodeBlock = !inCodeBlock;
        continue;
      }
      if (inCodeBlock) continue;

      const match = line.match(/^(#{1,6})\s+(.*)$/);
      if (match) {
        const level = match[1].length;
        const text = match[2].trim().replace(/\*|_/g, ""); // Remove basic bold/italic formatting
        const id = generateSlug(text);
        headings.push({ id, text, level });
      }
    }
    return headings;
  }, [markdown]);

  useEffect(() => {
    if (toc.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        // Find all intersecting entries
        const intersecting = entries.filter((entry) => entry.isIntersecting);
        if (intersecting.length > 0) {
          // If multiple headings are in the top 20% viewport, pick the topmost one
          const topMost = intersecting.reduce((prev, curr) =>
            prev.boundingClientRect.top < curr.boundingClientRect.top
              ? prev
              : curr,
          );
          setActiveId(topMost.target.id);
        }
      },
      // The top 20% of the viewport is the trigger zone.
      { rootMargin: "0% 0px -80% 0px" },
    );

    const elements = toc
      .map((entry) => document.getElementById(entry.id))
      .filter((el): el is HTMLElement => el !== null);

    elements.forEach((el) => observer.observe(el));

    return () => {
      elements.forEach((el) => observer.unobserve(el));
      observer.disconnect();
    };
  }, [toc]);

  if (toc.length === 0) return null;

  return (
    <div className={cn("text-sm", className)}>
      <div className="mb-6 flex items-center gap-2">
        <Menu className="h-4 w-4 text-muted-foreground" />
        <span className="font-medium text-foreground">On this page</span>
      </div>
      <ul className="flex flex-col gap-3">
        {toc.map((entry, index) => (
          <li
            key={`${entry.id}-${index}`}
            style={{ paddingLeft: `${(entry.level - 2) * 1.25}rem` }}
          >
            <a
              href={`#${entry.id}`}
              className={cn(
                "block transition-colors hover:text-foreground",
                activeId === entry.id
                  ? "font-semibold text-primary"
                  : "text-muted-foreground",
              )}
            >
              {entry.text}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
