import { useState, useCallback, useRef, useMemo } from "react";
import { usePersistedState } from "@/hooks/aevr/use-persisted-state";
import { logger } from "@untools/logger";

export type StatusState = "loading" | "success" | "error" | "warning" | "idle";

export interface StatusItem {
  name?: string;
  status?: StatusState;
  message?: string;
  key?: string;
  timestamp?: number;
}

export type StatusRecord<K extends string = string> = Record<K, StatusItem>;

export interface AggregatedStatus {
  title: string;
  description: string;
  type: StatusState;
  loading: boolean;
}

interface UseStatusOptions<K extends string = string> {
  strategy?: "local" | "global";
  namespace?: string;
  defaultStatuses?: StatusRecord<K>;
  // New aggregation options
  aggregation?: {
    enabled?: boolean;
    priority?: K[]; // Order of priority for display
    defaultTitle?: string;
    defaultDescription?: string;
  };
  // Persistence options (only applies to global strategy)
  persistence?: {
    enabled?: boolean;
    storage?: Storage;
  };
}

// Global store interface for managing namespaced status data
interface GlobalStatusStore {
  namespaced: Record<string, StatusRecord>;
}

export const useStatus = <K extends string = string>(
  options: UseStatusOptions<K | string> = {},
) => {
  const {
    strategy = "local",
    namespace = "default",
    defaultStatuses = {} as StatusRecord<K>,
    aggregation = {},
    persistence = {},
  } = options;

  const {
    enabled: aggregationEnabled = false,
    priority = [],
    defaultTitle = "",
    defaultDescription = "",
  } = aggregation;

  const {
    enabled: persistenceEnabled = false,
    storage = typeof window !== "undefined" ? window.localStorage : undefined,
  } = persistence;

  // Track if we've initialized defaults to avoid re-initializing on re-renders
  const defaultsInitialized = useRef(false);

  // Create initial state with defaults (add timestamps to default items)
  const createInitialState = useCallback((): StatusRecord<K> => {
    const initialState = {} as StatusRecord<K>;
    Object.entries(defaultStatuses).forEach(([key, item]) => {
      (initialState as Record<string, StatusItem>)[key] = {
        ...(item as StatusItem),
        timestamp: (item as StatusItem).timestamp || Date.now(),
      };
    });
    return initialState;
  }, [defaultStatuses]);

  // Local state with defaults
  const [localStatus, setLocalStatus] =
    useState<StatusRecord<K>>(createInitialState);

  // Global state management using usePersistedState
  const globalStoreKey = "status-store";
  const globalInitialState: GlobalStatusStore = { namespaced: {} };

  // Always call usePersistedState - it handles existing stores internally
  const { state: globalStore, setState: setGlobalStore } = usePersistedState(
    globalInitialState,
    {
      storageKey: globalStoreKey,
      enablePersistence: persistenceEnabled,
      storage,
    },
  );

  // Get current namespace status from global store
  const globalStatus = useMemo(() => {
    return globalStore.namespaced[namespace] || {};
  }, [globalStore.namespaced, namespace]);

  // Initialize namespace if it doesn't exist
  const initializeNamespace = useCallback(() => {
    if (!globalStore.namespaced[namespace]) {
      setGlobalStore((prev) => ({
        ...prev,
        namespaced: {
          ...prev.namespaced,
          [namespace]: {},
        },
      }));
    }
  }, [globalStore.namespaced, namespace, setGlobalStore]);

  // Initialize namespace on first use if using global strategy
  if (strategy === "global") {
    initializeNamespace();
  }

  // Initialize defaults for global strategy
  if (
    strategy === "global" &&
    !defaultsInitialized.current &&
    Object.keys(defaultStatuses).length > 0
  ) {
    const namespacedStatuses = globalStore.namespaced[namespace] || {};
    const hasDefaults = Object.keys(defaultStatuses).some(
      (key) => namespacedStatuses[key] !== undefined,
    );

    if (!hasDefaults) {
      const statusesWithTimestamps = Object.entries(defaultStatuses).reduce(
        (acc, [key, item]) => {
          acc[key] = {
            ...(item as StatusItem),
            timestamp: (item as StatusItem).timestamp || Date.now(),
          };
          return acc;
        },
        {} as Record<string, StatusItem>,
      );

      setGlobalStore((prev) => ({
        ...prev,
        namespaced: {
          ...prev.namespaced,
          [namespace]: {
            ...namespacedStatuses,
            ...statusesWithTimestamps,
          },
        },
      }));
    }
    defaultsInitialized.current = true;
  }

  const status = useMemo<StatusRecord<K>>(
    () =>
      (strategy === "global"
        ? (globalStatus as StatusRecord<K>)
        : localStatus) as StatusRecord<K>,
    [strategy, globalStatus, localStatus],
  );

  const setStatus = useCallback(
    (key: K, item: Omit<StatusItem, "timestamp">) => {
      const statusItem: StatusItem = {
        ...item,
        timestamp: Date.now(),
      };

      if (strategy === "global") {
        setGlobalStore((prev) => ({
          ...prev,
          namespaced: {
            ...prev.namespaced,
            [namespace]: {
              ...prev.namespaced[namespace],
              [key]: {
                ...prev.namespaced[namespace]?.[key as string],
                ...statusItem,
              },
            },
          },
        }));
      } else {
        setLocalStatus(
          (prev) =>
            ({
              ...prev,
              [key]: {
                ...prev[key],
                ...statusItem,
              },
            }) as StatusRecord<K>,
        );
      }
    },
    [strategy, namespace, setGlobalStore],
  );

  const clearStatus = useCallback(
    (key?: K) => {
      if (strategy === "global") {
        setGlobalStore((prev) => {
          const namespacedData = prev.namespaced[namespace] || {};

          if (key) {
            const { [key as string]: removedItem, ...rest } = namespacedData;
            logger.debug("clearStatus", { key, removedItem, rest });
            return {
              ...prev,
              namespaced: {
                ...prev.namespaced,
                [namespace]: rest,
              },
            };
          } else {
            // Reset to defaults instead of empty when clearing all
            const defaultsWithTimestamps = Object.entries(
              defaultStatuses,
            ).reduce(
              (acc, [defaultKey, item]) => {
                acc[defaultKey] = {
                  ...(item as StatusItem),
                  timestamp: (item as StatusItem).timestamp || Date.now(),
                };
                return acc;
              },
              {} as Record<string, StatusItem>,
            );

            return {
              ...prev,
              namespaced: {
                ...prev.namespaced,
                [namespace]: defaultsWithTimestamps,
              },
            };
          }
        });
      } else {
        if (key) {
          setLocalStatus((prev) => {
            const { [key]: removedItem, ...rest } = prev;
            logger.debug("clearStatus", { key, removedItem, rest });
            return rest as StatusRecord<K>;
          });
        } else {
          setLocalStatus(createInitialState()); // Reset to defaults instead of empty
        }
      }
    },
    [strategy, namespace, setGlobalStore, defaultStatuses, createInitialState],
  );

  const isLoading = (keys?: K[]) => {
    const targetKeys = keys || (Object.keys(status) as K[]);
    return targetKeys.some((key) => status[key]?.status === "loading");
  };

  const hasError = (keys?: K[]) => {
    const targetKeys = keys || (Object.keys(status) as K[]);
    return targetKeys.some((key) => status[key]?.status === "error");
  };

  const allSuccess = (keys?: K[]) => {
    const targetKeys = keys || (Object.keys(status) as K[]);
    return (
      targetKeys.length > 0 &&
      targetKeys.every((key) => status[key]?.status === "success")
    );
  };

  // New: Built-in aggregation logic
  const aggregatedStatus = useMemo((): AggregatedStatus => {
    if (!aggregationEnabled) {
      return {
        title: defaultTitle,
        description: defaultDescription,
        type: "idle",
        loading: false,
      };
    }

    const statusEntries = Object.entries(status).filter(
      ([, statusItem]) => statusItem,
    );

    if (statusEntries.length === 0) {
      return {
        title: defaultTitle,
        description: defaultDescription,
        type: "idle",
        loading: false,
      };
    }

    // Sort by priority if provided, otherwise by timestamp (most recent first), then by status importance
    const sortedStatuses = statusEntries.sort(([keyA], [keyB]) => {
      if (priority.length > 0) {
        const indexA = priority.indexOf(keyA as K);
        const indexB = priority.indexOf(keyB as K);
        if (indexA !== -1 && indexB !== -1) return indexA - indexB;
        if (indexA !== -1) return -1;
        if (indexB !== -1) return 1;
      }

      // Use timestamp as primary sort (most recent first)
      const timestampA = status[keyA as K]?.timestamp || 0;
      const timestampB = status[keyB as K]?.timestamp || 0;
      if (timestampA !== timestampB) {
        return timestampB - timestampA; // Most recent first
      }

      // Fallback to status priority: loading > error > warning > success > idle
      const statusPriority = {
        loading: 0,
        error: 1,
        warning: 2,
        success: 3,
        idle: 4,
      };
      const priorityA = statusPriority[status[keyA as K]?.status || "idle"];
      const priorityB = statusPriority[status[keyB as K]?.status || "idle"];
      return priorityA - priorityB;
    });

    const [, primaryStatus] = sortedStatuses[0];
    const hasLoadingStatus = statusEntries.some(
      ([, statusItem]) => (statusItem as StatusItem)?.status === "loading",
    );

    return {
      title: (primaryStatus as StatusItem)?.name || defaultTitle,
      description: (primaryStatus as StatusItem)?.message || defaultDescription,
      type: (primaryStatus as StatusItem)?.status || "idle",
      loading: hasLoadingStatus,
    };
  }, [status, aggregationEnabled, priority, defaultTitle, defaultDescription]);

  return {
    status,
    setStatus,
    clearStatus,
    isLoading,
    hasError,
    allSuccess,
    // New: Include aggregated status when enabled
    ...(aggregationEnabled && { aggregatedStatus }),
  };
};

// Legacy aggregator hook for backward compatibility
export const useStatusAggregator = (
  statuses: Record<string, StatusItem | undefined>,
  config: {
    priority?: string[];
    defaultTitle?: string;
    defaultDescription?: string;
  } = {},
): AggregatedStatus => {
  const { priority = [], defaultTitle = "", defaultDescription = "" } = config;

  return useMemo(() => {
    const statusEntries = Object.entries(statuses).filter(
      ([, status]) => status,
    );

    if (statusEntries.length === 0) {
      return {
        title: defaultTitle,
        description: defaultDescription,
        type: "idle",
        loading: false,
      };
    }

    // Sort by priority if provided, otherwise by timestamp (most recent first), then by status importance
    const sortedStatuses = statusEntries.sort(([keyA], [keyB]) => {
      if (priority.length > 0) {
        const indexA = priority.indexOf(keyA);
        const indexB = priority.indexOf(keyB);
        if (indexA !== -1 && indexB !== -1) return indexA - indexB;
        if (indexA !== -1) return -1;
        if (indexB !== -1) return 1;
      }

      // Use timestamp as primary sort (most recent first)
      const timestampA = statuses[keyA]?.timestamp || 0;
      const timestampB = statuses[keyB]?.timestamp || 0;
      if (timestampA !== timestampB) {
        return timestampB - timestampA; // Most recent first
      }

      // Fallback to status priority: loading > error > warning > success > idle
      const statusPriority = {
        loading: 0,
        error: 1,
        warning: 2,
        success: 3,
        idle: 4,
      };
      const priorityA = statusPriority[statuses[keyA]?.status || "idle"];
      const priorityB = statusPriority[statuses[keyB]?.status || "idle"];
      return priorityA - priorityB;
    });

    const [, primaryStatus] = sortedStatuses[0];
    const hasLoading = statusEntries.some(
      ([, status]) => status?.status === "loading",
    );

    return {
      title: primaryStatus?.name || defaultTitle,
      description: primaryStatus?.message || defaultDescription,
      type: primaryStatus?.status || "idle",
      loading: hasLoading,
    };
  }, [statuses, priority, defaultTitle, defaultDescription]);
};
