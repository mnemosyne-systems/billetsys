import { create, StateCreator, StoreApi, UseBoundStore } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

/** Helper alias for a bound store of a given state */
type BoundStore<S> = UseBoundStore<StoreApi<S>>;

/**
 * Registry to track created stores and prevent duplicates
 * Key: storageKey, Value: zustand bound store instance
 */
const storeRegistry = new Map<string, BoundStore<unknown>>();

/**
 * Internal store structure that wraps user state with persistence metadata
 */
interface PersistedStore<TState> {
  userState: TState;
  isHydrated: boolean;
  setUserState: (newState: TState | ((prev: TState) => TState)) => void;
  resetToInitial: () => void;
  markAsHydrated: (hydrated: boolean) => void;
}

/**
 * Configuration options for persisted state behavior
 */
export interface PersistedStateOptions<TState = unknown> {
  storageKey: string;
  enablePersistence?: boolean;
  selectForPersistence?: (state: TState) => Partial<TState>;
  storage?: Storage;
}

/**
 * Return type for the usePersistedState hook
 */
export interface PersistedStateResult<TState> {
  state: TState;
  setState: (newState: TState | ((prev: TState) => TState)) => void;
  resetState: () => void;
  isHydrated: boolean;
}

/**
 * A useState-like hook that provides global, optionally persisted state.
 */
export const usePersistedState = <TState = unknown>(
  initialStateOrOptions: TState | PersistedStateOptions<TState>,
  optionsParam?: PersistedStateOptions<TState>,
): PersistedStateResult<TState> => {
  // Handle overloaded parameters
  let initialState: TState;
  let options: PersistedStateOptions<TState>;

  if (optionsParam) {
    initialState = initialStateOrOptions as TState;
    options = optionsParam;
  } else {
    initialState = undefined as TState;
    options = initialStateOrOptions as PersistedStateOptions<TState>;
  }

  const {
    storageKey,
    enablePersistence = true,
    selectForPersistence,
    storage = typeof window !== "undefined" ? window.localStorage : undefined,
  } = options;

  if (!storageKey) {
    throw new Error("usePersistedState: storageKey is required");
  }

  // Return existing store if already created
  if (storeRegistry.has(storageKey)) {
    const existingStore = storeRegistry.get(storageKey)! as BoundStore<
      PersistedStore<TState>
    >;
    return createStoreInterface<TState>(existingStore);
  }

  // Create new store
  const newStore = createPersistedStore<TState>(
    initialState,
    storageKey,
    enablePersistence,
    selectForPersistence,
    storage,
  );

  // Register the store for future access
  storeRegistry.set(storageKey, newStore as BoundStore<unknown>);

  return createStoreInterface<TState>(newStore);
};

/**
 * Access an existing persisted state store from any component.
 */
export const useExistingPersistedState = <TState = unknown>(
  storageKey: string,
): PersistedStateResult<TState> => {
  const store = storeRegistry.get(storageKey) as
    | BoundStore<PersistedStore<TState>>
    | undefined;

  if (!store) {
    throw new Error(
      `useExistingPersistedState: No store found with key "${storageKey}". ` +
        `Make sure to create it first using usePersistedState.`,
    );
  }

  return createStoreInterface<TState>(store);
};

/**
 * Check if a persisted state store exists for the given key.
 */
export const hasPersistedState = (storageKey: string): boolean => {
  return storeRegistry.has(storageKey);
};

/**
 * Remove a persisted state store and clear its data from storage.
 */
export const removePersistedState = (storageKey: string): boolean => {
  const wasRemoved = storeRegistry.delete(storageKey);

  if (wasRemoved && typeof window !== "undefined") {
    try {
      window.localStorage.removeItem(storageKey);
    } catch (error) {
      console.warn(
        `Failed to remove persisted data for key "${storageKey}":`,
        error,
      );
    }
  }

  return wasRemoved;
};

/**
 * Get a list of all registered store keys.
 */
export const getPersistedStateKeys = (): string[] => {
  return Array.from(storeRegistry.keys());
};

/**
 * Clear all persisted state stores and their storage data.
 */
export const clearAllPersistedState = (): void => {
  const keys = getPersistedStateKeys();

  // Clear from registry
  storeRegistry.clear();

  // Clear from localStorage
  if (typeof window !== "undefined") {
    keys.forEach((key) => {
      try {
        window.localStorage.removeItem(key);
      } catch (error) {
        console.warn(`Failed to clear persisted data for key "${key}":`, error);
      }
    });
  }
};

// ------- Internal Implementation Details -------

/**
 * Creates a new Zustand store with optional persistence
 */
function createPersistedStore<TState>(
  initialState: TState,
  storageKey: string,
  enablePersistence: boolean,
  selectForPersistence?: (state: TState) => Partial<TState>,
  storage?: Storage,
): BoundStore<PersistedStore<TState>> {
  const storeCreator: StateCreator<
    PersistedStore<TState>,
    [],
    [],
    PersistedStore<TState>
  > = (set, get) => ({
    userState: initialState,
    isHydrated: false,

    setUserState: (newState) => {
      set((current) => ({
        userState:
          typeof newState === "function"
            ? (newState as (prev: TState) => TState)(current.userState)
            : newState,
      }));
    },

    resetToInitial: () => {
      set({
        userState: initialState,
        isHydrated: get().isHydrated,
      });
    },

    markAsHydrated: (hydrated) => {
      set({ isHydrated: hydrated });
    },
  });

  if (enablePersistence && storage) {
    // Persisted store
    return create<PersistedStore<TState>>()(
      persist(storeCreator, {
        name: storageKey,
        storage: createJSONStorage(() => storage),
        partialize: selectForPersistence
          ? (state) => ({ userState: selectForPersistence(state.userState) })
          : (state) => ({ userState: state.userState }),
        onRehydrateStorage: () => (state) => {
          state?.markAsHydrated(true);
        },
      }),
    );
  } else {
    // Non-persisted store
    const store = create<PersistedStore<TState>>()(storeCreator);
    store.getState().markAsHydrated(true);
    return store;
  }
}

/**
 * Creates the public interface that consumers interact with
 */
function createStoreInterface<TState>(
  store: BoundStore<PersistedStore<TState>>,
): PersistedStateResult<TState> {
  return {
    state: store((state) => state.userState),
    setState: store((state) => state.setUserState),
    resetState: store((state) => state.resetToInitial),
    isHydrated: store((state) => state.isHydrated),
  };
}
