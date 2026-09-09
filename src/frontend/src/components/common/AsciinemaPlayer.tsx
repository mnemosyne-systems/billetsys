/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useRef } from "react";
import { create } from "asciinema-player";
import "asciinema-player/dist/bundle/asciinema-player.css";

interface AsciinemaPlayerProps {
  src: string;
  className?: string;
}

export default function AsciinemaPlayer({
  src,
  className,
}: AsciinemaPlayerProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const player = create(src, containerRef.current, {
      controls: true,
      fit: "width",
    });

    return () => {
      player.dispose();
    };
  }, [src]);

  return <div ref={containerRef} className={className} />;
}
