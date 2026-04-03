/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

interface LevelColorProps {
  color?: string | null;
  display?: string | null;
}

export function LevelColorBadge({ color, display }: LevelColorProps) {
  return (
    <span className="status-pill level-color-badge">
      <span
        className="level-color-swatch"
        style={{ backgroundColor: color || "transparent" }}
        aria-hidden="true"
      />
      <span>{display || "No color"}</span>
    </span>
  );
}

export function LevelColorFieldValue({ color, display }: LevelColorProps) {
  return (
    <div className="level-color-field-value">
      <span
        className="level-color-swatch"
        style={{ backgroundColor: color || "transparent" }}
        aria-hidden="true"
      />
      <span>{display || "—"}</span>
    </div>
  );
}
