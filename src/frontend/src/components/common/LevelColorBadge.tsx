interface LevelColorProps {
  color?: string | null;
  display?: string | null;
}

export function LevelColorBadge({ color, display }: LevelColorProps) {
  return (
    <span className="status-pill level-color-badge">
      <span className="level-color-swatch" style={{ backgroundColor: color || 'transparent' }} aria-hidden="true" />
      <span>{display || 'No color'}</span>
    </span>
  );
}

export function LevelColorFieldValue({ color, display }: LevelColorProps) {
  return (
    <div className="level-color-field-value">
      <span className="level-color-swatch" style={{ backgroundColor: color || 'transparent' }} aria-hidden="true" />
      <span>{display || '—'}</span>
    </div>
  );
}
