export function caretFromPoint(
  x: number,
  y: number,
): null | {
  offset: number;
  node: Node;
} {
  const documentWithCaretPosition = document as Document & {
    caretPositionFromPoint?: (
      xPos: number,
      yPos: number,
    ) => { offsetNode: Node; offset: number } | null;
  };

  if (typeof document.caretRangeFromPoint !== "undefined") {
    const range = document.caretRangeFromPoint(x, y);
    if (range === null) {
      return null;
    }
    return {
      node: range.startContainer,
      offset: range.startOffset,
    };
  } else if (
    typeof documentWithCaretPosition.caretPositionFromPoint !== "undefined"
  ) {
    const range = documentWithCaretPosition.caretPositionFromPoint(x, y);
    if (range === null) {
      return null;
    }
    return {
      node: range.offsetNode,
      offset: range.offset,
    };
  } else {
    // Gracefully handle IE
    return null;
  }
}
