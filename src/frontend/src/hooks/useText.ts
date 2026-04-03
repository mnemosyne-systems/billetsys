/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";

interface TextState {
  loading: boolean;
  error: string;
  data: string;
}

interface TextSnapshot extends TextState {
  url: string;
}

export default function useText(url: string): TextState {
  const [state, setState] = useState<TextSnapshot>({
    url: "",
    loading: false,
    error: "",
    data: "",
  });

  useEffect(() => {
    if (!url) {
      return undefined;
    }

    let active = true;

    fetch(url, { credentials: "same-origin", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Unable to load ${url}`);
        }
        return response.text();
      })
      .then((data) => {
        if (active) {
          setState({ url, loading: false, error: "", data });
        }
      })
      .catch((error: unknown) => {
        if (active) {
          const message =
            error instanceof Error ? error.message : "Unable to load data";
          setState({
            url,
            loading: false,
            error: message,
            data: "",
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  if (!url) {
    return { loading: false, error: "", data: "" };
  }

  if (state.url !== url) {
    return { loading: true, error: "", data: "" };
  }

  return state;
}
