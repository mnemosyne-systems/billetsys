import type { ReactNode } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';

interface MarkdownContentProps {
  children?: ReactNode;
}

export default function MarkdownContent({ children }: MarkdownContentProps) {
  return <ReactMarkdown rehypePlugins={[rehypeHighlight]}>{typeof children === 'string' ? children : ''}</ReactMarkdown>;
}
