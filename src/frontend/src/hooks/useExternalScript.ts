/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";

interface ExternalScriptState {
  loaded: boolean;
  error: string;
}

interface ExternalScriptSnapshot extends ExternalScriptState {
  src: string;
}

function getScriptSnapshot(src: string): ExternalScriptSnapshot {
  if (!src) {
    return { src, loaded: false, error: "" };
  }
  if (document.querySelector(`script[src="${src}"]`) && window.Chart) {
    return { src, loaded: true, error: "" };
  }
  return { src, loaded: false, error: "" };
}

export default function useExternalScript(src: string): ExternalScriptState {
  const [state, setState] = useState<ExternalScriptSnapshot>(() =>
    getScriptSnapshot(src),
  );
  const resolvedState = state.src === src ? state : getScriptSnapshot(src);

  useEffect(() => {
    if (!src) {
      return undefined;
    }
    if (document.querySelector(`script[src="${src}"]`) && window.Chart) {
      return undefined;
    }

    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      const onLoad = () => setState({ src, loaded: true, error: "" });
      const onError = () =>
        setState({ src, loaded: false, error: `Unable to load ${src}` });
      existing.addEventListener("load", onLoad);
      existing.addEventListener("error", onError);
      return () => {
        existing.removeEventListener("load", onLoad);
        existing.removeEventListener("error", onError);
      };
    }

    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    const onLoad = () => setState({ src, loaded: true, error: "" });
    const onError = () =>
      setState({ src, loaded: false, error: `Unable to load ${src}` });
    script.addEventListener("load", onLoad);
    script.addEventListener("error", onError);
    document.body.appendChild(script);

    return () => {
      script.removeEventListener("load", onLoad);
      script.removeEventListener("error", onError);
    };
  }, [src]);

  return {
    loaded: resolvedState.loaded,
    error: resolvedState.error,
  };
}
