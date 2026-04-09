/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Component, type ErrorInfo, type ReactNode } from "react";

interface RouteErrorBoundaryProps {
  children?: ReactNode;
}

interface RouteErrorBoundaryState {
  error: Error | null;
}

export default class RouteErrorBoundary extends Component<
  RouteErrorBoundaryProps,
  RouteErrorBoundaryState
> {
  constructor(props: RouteErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): RouteErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (typeof console !== "undefined" && typeof console.error === "function") {
      console.error("Route rendering failed", error, info);
    }
  }

  render() {
    if (this.state.error) {
      return (
        <section className="w-full max-w-5xl mx-auto mt-6">
          <div className="flex flex-col items-center justify-center p-12 text-center border rounded-lg bg-card text-card-foreground">
            <h1 className="text-4xl font-bold tracking-tight mb-4 text-destructive">
              Error
            </h1>
            <p className="error-text">
              {this.state.error?.message ||
                "A route-level error occurred while loading the page."}
            </p>
          </div>
        </section>
      );
    }

    return this.props.children;
  }
}
