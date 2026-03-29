import { useEffect, useState } from 'react';

interface TextState {
  loading: boolean;
  error: string;
  data: string;
}

export default function useText(url: string): TextState {
  const [state, setState] = useState<TextState>({
    loading: Boolean(url),
    error: '',
    data: ''
  });

  useEffect(() => {
    if (!url) {
      setState({ loading: false, error: '', data: '' });
      return undefined;
    }

    let active = true;
    setState(current => ({ ...current, loading: true, error: '' }));

    fetch(url, { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
        if (!response.ok) {
          throw new Error(`Unable to load ${url}`);
        }
        return response.text();
      })
      .then(data => {
        if (active) {
          setState({ loading: false, error: '', data });
        }
      })
      .catch((error: unknown) => {
        if (active) {
          const message = error instanceof Error ? error.message : 'Unable to load data';
          setState({
            loading: false,
            error: message,
            data: ''
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  return state;
}
