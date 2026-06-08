/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";

interface ChapterSummary {
  slug: string;
  title: string;
}

interface DocsPagerProps {
  chapters: ChapterSummary[];
  activeSlug: string;
  className?: string;
}

export default function DocsPager({
  chapters,
  activeSlug,
  className,
}: DocsPagerProps) {
  const currentIndex = chapters.findIndex((c) => c.slug === activeSlug);

  const prev = currentIndex > 0 ? chapters[currentIndex - 1] : null;
  const next =
    currentIndex !== -1 && currentIndex < chapters.length - 1
      ? chapters[currentIndex + 1]
      : null;

  if (!prev && !next) return null;

  return (
    <div
      className={cn(
        "flex flex-row items-center justify-between font-medium",
        className,
      )}
    >
      {prev ? (
        <Link
          to={`/manual/${prev.slug}`}
          className="flex items-center gap-2 text-foreground transition-colors hover:text-muted-foreground"
        >
          <ChevronLeft className="h-4 w-4" />
          <span>{prev.title}</span>
        </Link>
      ) : (
        <div />
      )}
      {next ? (
        <Link
          to={`/manual/${next.slug}`}
          className="flex items-center gap-2 text-foreground transition-colors hover:text-muted-foreground"
        >
          <span>{next.title}</span>
          <ChevronRight className="h-4 w-4" />
        </Link>
      ) : (
        <div />
      )}
    </div>
  );
}
