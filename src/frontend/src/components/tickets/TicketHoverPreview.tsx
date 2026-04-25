import type { MouseEvent, ReactNode } from "react";
import { useState } from "react";

interface TooltipState {
  left: number;
  top: number;
}

interface TicketHoverPreviewProps {
  ticketName: string;
  ticketTitle: string;
  status?: string;
  companyName?: string | null;
  levelName?: string | null;
  detailPath: string;
  children: ReactNode;
  className?: string;
}

export function TicketHoverPreview({
  ticketName,
  ticketTitle,
  detailPath,
  children,
  className,
}: TicketHoverPreviewProps) {
  const [tooltipState, setTooltipState] = useState<TooltipState | null>(null);

  const updateTooltip = (event: MouseEvent<HTMLAnchorElement>) => {
    const pad = 12;
    const width = 260;
    const height = 120;
    let left = event.clientX + pad;
    let top = event.clientY + pad;
    if (left + width > window.innerWidth - pad) {
      left = event.clientX - width - pad;
    }
    if (top + height > window.innerHeight - pad) {
      top = event.clientY - height - pad;
    }
    setTooltipState({ left, top });
  };

  return (
    <>
      <a
        className={`text-primary hover:underline font-medium ${className || ""}`}
        href={detailPath}
        target="_blank"
        rel="noreferrer"
        onMouseEnter={updateTooltip}
        onMouseMove={updateTooltip}
        onMouseLeave={() => setTooltipState(null)}
        onBlur={() => setTooltipState(null)}
      >
        {children}
      </a>
      {tooltipState && (
        <div
          className="fixed z-50 pointer-events-none"
          style={{ left: tooltipState.left, top: tooltipState.top }}
        >
          <div className="bg-popover text-popover-foreground border shadow-md rounded-lg p-3 w-[260px] flex flex-col space-y-1.5 animate-in fade-in zoom-in-95 duration-100">
            <div className="text-sm font-semibold truncate">#{ticketName}</div>
            <div className="text-xs text-muted-foreground line-clamp-2">
              {ticketTitle}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
