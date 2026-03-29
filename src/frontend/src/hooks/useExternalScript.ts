import { useEffect, useState } from 'react';

interface ExternalScriptState {
  loaded: boolean;
  error: string;
}

export default function useExternalScript(src: string): ExternalScriptState {
  const [state, setState] = useState<ExternalScriptState>({
    loaded: false,
    error: ''
  });

  useEffect(() => {
    if (!src) {
      setState({ loaded: false, error: '' });
      return undefined;
    }
    if (document.querySelector(`script[src="${src}"]`) && window.Chart) {
      setState({ loaded: true, error: '' });
      return undefined;
    }

    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      const onLoad = () => setState({ loaded: true, error: '' });
      const onError = () => setState({ loaded: false, error: `Unable to load ${src}` });
      existing.addEventListener('load', onLoad);
      existing.addEventListener('error', onError);
      return () => {
        existing.removeEventListener('load', onLoad);
        existing.removeEventListener('error', onError);
      };
    }

    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    const onLoad = () => setState({ loaded: true, error: '' });
    const onError = () => setState({ loaded: false, error: `Unable to load ${src}` });
    script.addEventListener('load', onLoad);
    script.addEventListener('error', onError);
    document.body.appendChild(script);

    return () => {
      script.removeEventListener('load', onLoad);
      script.removeEventListener('error', onError);
    };
  }, [src]);

  return state;
}
