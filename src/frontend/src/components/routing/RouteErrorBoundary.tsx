import { Component, type ErrorInfo, type ReactNode } from 'react';

interface RouteErrorBoundaryProps {
  children?: ReactNode;
}

interface RouteErrorBoundaryState {
  error: Error | null;
}

export default class RouteErrorBoundary extends Component<RouteErrorBoundaryProps, RouteErrorBoundaryState> {
  constructor(props: RouteErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): RouteErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, _info: ErrorInfo) {
    if (typeof console !== 'undefined' && typeof console.error === 'function') {
      console.error('Route rendering failed', error);
    }
  }

  render() {
    if (this.state.error) {
      return (
        <section className="panel">
          <h2>Unable to load this page</h2>
          <p className="error-text">{this.state.error?.message || 'A route-level error occurred while loading the page.'}</p>
        </section>
      );
    }

    return this.props.children;
  }
}
