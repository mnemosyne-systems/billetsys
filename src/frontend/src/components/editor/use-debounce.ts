import { useMemo } from "react";

import { debounce } from "lodash";

export function useDebounce<T extends (...args: never[]) => void>(
  fn: T,
  ms: number,
  maxWait?: number,
) {
  return useMemo(() => debounce(fn, ms, { maxWait }), [fn, ms, maxWait]);
}
