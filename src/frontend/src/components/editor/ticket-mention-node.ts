import {
  $applyNodeReplacement,
  type DOMConversionMap,
  type DOMConversionOutput,
  type DOMExportOutput,
  type EditorConfig,
  type LexicalNode,
  type NodeKey,
  type SerializedTextNode,
  type Spread,
  TextNode,
} from "lexical";

export type SerializedTicketMentionNode = Spread<
  {
    ticketId: number;
    ticketName: string;
    ticketTitle: string;
  },
  SerializedTextNode
>;

function $convertTicketMentionElement(
  domNode: HTMLElement,
): DOMConversionOutput | null {
  const ticketId = domNode.getAttribute("data-ticket-id");
  const ticketName = domNode.getAttribute("data-ticket-name");
  const textContent = domNode.textContent;

  if (ticketId !== null && textContent !== null) {
    const node = $createTicketMentionNode(
      Number(ticketId),
      ticketName || textContent,
      "",
    );
    return { node };
  }

  return null;
}

const ticketMentionStyle =
  "background-color: rgba(130, 80, 223, 0.15); border-radius: 3px; padding: 0 4px;";

export class TicketMentionNode extends TextNode {
  __ticketId: number;
  __ticketName: string;
  __ticketTitle: string;

  static getType(): string {
    return "ticket-mention";
  }

  static clone(node: TicketMentionNode): TicketMentionNode {
    return new TicketMentionNode(
      node.__ticketId,
      node.__ticketName,
      node.__ticketTitle,
      node.__text,
      node.__key,
    );
  }

  static importJSON(
    serializedNode: SerializedTicketMentionNode,
  ): TicketMentionNode {
    const node = $createTicketMentionNode(
      serializedNode.ticketId,
      serializedNode.ticketName,
      serializedNode.ticketTitle,
    );
    node.setTextContent(serializedNode.text);
    node.setFormat(serializedNode.format);
    node.setDetail(serializedNode.detail);
    node.setMode(serializedNode.mode);
    node.setStyle(serializedNode.style);
    return node;
  }

  constructor(
    ticketId: number,
    ticketName: string,
    ticketTitle: string,
    text?: string,
    key?: NodeKey,
  ) {
    super(text ?? `#${ticketName}`, key);
    this.__ticketId = ticketId;
    this.__ticketName = ticketName;
    this.__ticketTitle = ticketTitle;
  }

  exportJSON(): SerializedTicketMentionNode {
    return {
      ...super.exportJSON(),
      ticketId: this.__ticketId,
      ticketName: this.__ticketName,
      ticketTitle: this.__ticketTitle,
      type: "ticket-mention",
      version: 1,
    };
  }

  getTicketId(): number {
    return this.__ticketId;
  }

  getTicketName(): string {
    return this.__ticketName;
  }

  createDOM(config: EditorConfig): HTMLElement {
    const dom = super.createDOM(config);
    dom.style.cssText = ticketMentionStyle;
    dom.className = "ticket-mention";
    return dom;
  }

  exportDOM(): DOMExportOutput {
    const element = document.createElement("span");
    element.setAttribute("data-lexical-ticket-mention", "true");
    element.setAttribute("data-ticket-id", String(this.__ticketId));
    element.setAttribute("data-ticket-name", this.__ticketName);
    element.textContent = this.__text;
    return { element };
  }

  static importDOM(): DOMConversionMap | null {
    return {
      span: (domNode: HTMLElement) => {
        if (!domNode.hasAttribute("data-lexical-ticket-mention")) {
          return null;
        }
        return {
          conversion: $convertTicketMentionElement,
          priority: 1,
        };
      },
    };
  }

  isTextEntity(): true {
    return true;
  }

  canInsertTextBefore(): boolean {
    return false;
  }

  canInsertTextAfter(): boolean {
    return false;
  }
}

export function $createTicketMentionNode(
  ticketId: number,
  ticketName: string,
  ticketTitle: string,
): TicketMentionNode {
  const node = new TicketMentionNode(ticketId, ticketName, ticketTitle);
  node.setMode("segmented").toggleDirectionless();
  return $applyNodeReplacement(node);
}

export function $isTicketMentionNode(
  node: LexicalNode | null | undefined,
): node is TicketMentionNode {
  return node instanceof TicketMentionNode;
}
