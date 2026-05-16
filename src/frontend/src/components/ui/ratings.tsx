/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import * as React from "react";
import { Star } from "lucide-react";

import { cn } from "@/lib/utils";

const ratingVariants = {
  default: {
    star: "text-foreground",
    emptyStar: "text-muted-foreground",
  },
  destructive: {
    star: "text-destructive",
    emptyStar: "text-destructive/70",
  },
  yellow: {
    star: "text-yellow-500",
    emptyStar: "text-muted",
  },
};

type StarElementProps = React.SVGProps<SVGSVGElement> & {
  size?: number;
};

interface RatingsProps extends Omit<
  React.ComponentProps<"div">,
  "onChange" | "defaultValue"
> {
  totalStars?: number;
  size?: number;
  fill?: boolean;
  icon?: React.ReactElement<StarElementProps>;
  variant?: keyof typeof ratingVariants;
  asInput?: boolean;
  value: number;
  onValueChange?: (value: number) => void;
}

function Ratings({
  totalStars = 10,
  size = 20,
  fill = true,
  icon = <Star />,
  variant = "yellow",
  asInput = false,
  onValueChange,
  value,
  className,
  ...props
}: RatingsProps) {
  const rating = Math.max(0, Math.min(value || 0, totalStars));
  const fullStars = Math.floor(rating);
  const hasPartial = rating % 1 > 0;
  const emptyCount = Math.max(0, totalStars - fullStars - (hasPartial ? 1 : 0));
  const selectedValue = asInput ? Math.round(rating) : 0;

  function handleSelect(starValue: number) {
    if (asInput) {
      onValueChange?.(starValue);
    }
  }

  function handleKeyDown(event: React.KeyboardEvent, starValue: number) {
    if (!asInput) {
      return;
    }
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleSelect(starValue);
    } else if (event.key === "ArrowRight" || event.key === "ArrowUp") {
      event.preventDefault();
      const next = Math.min(starValue + 1, totalStars);
      handleSelect(next);
    } else if (event.key === "ArrowLeft" || event.key === "ArrowDown") {
      event.preventDefault();
      const prev = Math.max(starValue - 1, 1);
      handleSelect(prev);
    }
  }

  function starTabIndex(starValue: number): number | undefined {
    if (!asInput) {
      return undefined;
    }
    if (selectedValue === 0) {
      return starValue === 1 ? 0 : -1;
    }
    return starValue === selectedValue ? 0 : -1;
  }

  return (
    <div
      data-slot="ratings"
      role={asInput ? "radiogroup" : undefined}
      aria-label={asInput ? "Rating" : `Rating: ${rating} out of ${totalStars}`}
      className={cn("flex items-center gap-1", className)}
      {...props}
    >
      {Array.from({ length: fullStars }, (_, i) => {
        const starValue = i + 1;
        return React.cloneElement(icon, {
          key: `filled-${i}`,
          size,
          className: cn(
            fill ? "fill-current" : "fill-transparent",
            ratingVariants[variant].star,
            asInput &&
              "cursor-pointer outline-none focus-visible:outline-2 focus-visible:outline-ring rounded-sm",
          ),
          role: asInput ? "radio" : undefined,
          "aria-checked": asInput ? starValue === selectedValue : undefined,
          "aria-label": asInput
            ? `${starValue} star${starValue !== 1 ? "s" : ""}`
            : undefined,
          tabIndex: starTabIndex(starValue),
          onClick: asInput ? () => handleSelect(starValue) : undefined,
          onKeyDown: asInput
            ? (e: React.KeyboardEvent) => handleKeyDown(e, starValue)
            : undefined,
        });
      })}
      {hasPartial && (
        <PartialStar
          fillPercentage={rating % 1}
          size={size}
          className={cn(ratingVariants[variant].star)}
          icon={icon}
          interactive={asInput}
          starValue={fullStars + 1}
          selected={fullStars + 1 === selectedValue}
          tabIndex={starTabIndex(fullStars + 1)}
          onClick={asInput ? () => handleSelect(fullStars + 1) : undefined}
          onKeyDown={
            asInput
              ? (e: React.KeyboardEvent) => handleKeyDown(e, fullStars + 1)
              : undefined
          }
        />
      )}
      {Array.from({ length: emptyCount }, (_, i) => {
        const starValue = fullStars + i + 1 + (hasPartial ? 1 : 0);
        return React.cloneElement(icon, {
          key: `empty-${starValue}`,
          size,
          className: cn(
            ratingVariants[variant].emptyStar,
            asInput &&
              "cursor-pointer outline-none focus-visible:outline-2 focus-visible:outline-ring rounded-sm",
          ),
          role: asInput ? "radio" : undefined,
          "aria-checked": asInput ? false : undefined,
          "aria-label": asInput
            ? `${starValue} star${starValue !== 1 ? "s" : ""}`
            : undefined,
          tabIndex: starTabIndex(starValue),
          onClick: asInput ? () => handleSelect(starValue) : undefined,
          onKeyDown: asInput
            ? (e: React.KeyboardEvent) => handleKeyDown(e, starValue)
            : undefined,
        });
      })}
    </div>
  );
}

interface PartialStarProps {
  fillPercentage: number;
  size: number;
  className?: string;
  icon: React.ReactElement<StarElementProps>;
  interactive?: boolean;
  starValue: number;
  selected?: boolean;
  tabIndex?: number;
  onClick?: () => void;
  onKeyDown?: (event: React.KeyboardEvent) => void;
}

function PartialStar({
  fillPercentage,
  size,
  className,
  icon,
  interactive,
  starValue,
  selected,
  tabIndex,
  onClick,
  onKeyDown,
}: PartialStarProps) {
  return (
    <div
      role={interactive ? "radio" : undefined}
      aria-checked={interactive ? selected : undefined}
      aria-label={
        interactive
          ? `${starValue} star${starValue !== 1 ? "s" : ""}`
          : undefined
      }
      tabIndex={tabIndex}
      onClick={onClick}
      onKeyDown={onKeyDown}
      className={cn(
        "relative inline-block",
        interactive &&
          "cursor-pointer outline-none focus-visible:outline-2 focus-visible:outline-ring rounded-sm",
      )}
    >
      {React.cloneElement(icon, {
        size,
        className: cn("fill-transparent", className),
      })}
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          height: "100%",
          overflow: "hidden",
          width: `${fillPercentage * 100}%`,
        }}
      >
        {React.cloneElement(icon, {
          size,
          className: cn("fill-current", className),
        })}
      </div>
    </div>
  );
}

export { Ratings };
