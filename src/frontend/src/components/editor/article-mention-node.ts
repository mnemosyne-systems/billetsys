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

export type SerializedArticleMentionNode = Spread<
  {
    articleId: number;
    articleName: string;
    articleTitle: string;
  },
  SerializedTextNode
>;

function $convertArticleMentionElement(
  domNode: HTMLElement,
): DOMConversionOutput | null {
  const articleId = domNode.getAttribute("data-article-id");
  const articleName = domNode.getAttribute("data-article-name");
  const textContent = domNode.textContent;

  if (articleId !== null && textContent !== null) {
    const node = $createArticleMentionNode(
      Number(articleId),
      articleName || textContent,
      "",
    );
    return { node };
  }

  return null;
}

const articleMentionStyle =
  "background-color: rgba(130, 80, 223, 0.15); border-radius: 3px; padding: 0 4px;";

export class ArticleMentionNode extends TextNode {
  __articleId: number;
  __articleName: string;
  __articleTitle: string;

  static getType(): string {
    return "article-mention";
  }

  static clone(node: ArticleMentionNode): ArticleMentionNode {
    return new ArticleMentionNode(
      node.__articleId,
      node.__articleName,
      node.__articleTitle,
      node.__text,
      node.__key,
    );
  }

  static importJSON(
    serializedNode: SerializedArticleMentionNode,
  ): ArticleMentionNode {
    const node = $createArticleMentionNode(
      serializedNode.articleId,
      serializedNode.articleName,
      serializedNode.articleTitle,
    );
    node.setTextContent(serializedNode.text);
    node.setFormat(serializedNode.format);
    node.setDetail(serializedNode.detail);
    node.setMode(serializedNode.mode);
    node.setStyle(serializedNode.style);
    return node;
  }

  constructor(
    articleId: number,
    articleName: string,
    articleTitle: string,
    text?: string,
    key?: NodeKey,
  ) {
    super(
      text ??
        (articleTitle && articleTitle.length > 0
          ? articleTitle
          : `$${articleName}`),
      key,
    );
    this.__articleId = articleId;
    this.__articleName = articleName;
    this.__articleTitle = articleTitle;
  }

  exportJSON(): SerializedArticleMentionNode {
    return {
      ...super.exportJSON(),
      articleId: this.__articleId,
      articleName: this.__articleName,
      articleTitle: this.__articleTitle,
      type: "article-mention",
      version: 1,
    };
  }

  getArticleId(): number {
    return this.__articleId;
  }

  getArticleName(): string {
    return this.__articleName;
  }

  createDOM(config: EditorConfig): HTMLElement {
    const dom = super.createDOM(config);
    dom.style.cssText = articleMentionStyle;
    dom.className = "article-mention";
    return dom;
  }

  exportDOM(): DOMExportOutput {
    const element = document.createElement("span");
    element.setAttribute("data-lexical-article-mention", "true");
    element.setAttribute("data-article-id", String(this.__articleId));
    element.setAttribute("data-article-name", this.__articleName);
    element.textContent = this.__text;
    return { element };
  }

  static importDOM(): DOMConversionMap | null {
    return {
      span: (domNode: HTMLElement) => {
        if (!domNode.hasAttribute("data-lexical-article-mention")) {
          return null;
        }
        return {
          conversion: $convertArticleMentionElement,
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

export function $createArticleMentionNode(
  articleId: number,
  articleName: string,
  articleTitle: string,
): ArticleMentionNode {
  const node = new ArticleMentionNode(articleId, articleName, articleTitle);
  node.setMode("segmented").toggleDirectionless();
  return $applyNodeReplacement(node);
}

export function $isArticleMentionNode(
  node: LexicalNode | null | undefined,
): node is ArticleMentionNode {
  return node instanceof ArticleMentionNode;
}
