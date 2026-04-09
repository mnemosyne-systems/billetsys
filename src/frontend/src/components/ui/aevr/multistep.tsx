"use client";

import React, { useState, ReactNode, useCallback, useEffect } from "react";
import { usePersistedState } from "@/hooks/aevr/use-persisted-state";
import { useStatus, StatusRecord } from "@/hooks/aevr/use-status";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Icon } from "iconsax-react";
import { motion, AnimatePresence } from "motion/react";

export interface MinimalStepProps<T = unknown> {
  values: T;
  setValues: (values: T | ((prev: T) => T)) => void;
  next: (values?: T) => void;
  prev: () => void;
  isFirst: boolean;
  isLast: boolean;
  status: StatusRecord;
  setStatus: (key: string, item: unknown) => void;
  goToStep: (step: number) => void;
  isProcessing?: boolean;
  stepInfo?: {
    currentStep: number;
    totalSteps: number;
  };
}

interface MinimalMultiStepProps<T> {
  options?: {
    showBackBtn?: boolean;
  };
  trigger?: ReactNode;
  icon?: Icon | ReactNode;
  title?: string;
  description?: string;
  steps: Array<(props: MinimalStepProps<T>) => ReactNode>;
  initialValues: T;
  storageKey?: string;
  onComplete?: (values: T) => Promise<void> | void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  persist?: boolean;
  stepProps?: Record<string, unknown>;
  isProcessing?: boolean;
}

export function MinimalMultiStep<T>({
  icon,
  title,
  description,
  steps,
  initialValues,
  storageKey,
  onComplete,
  open: controlledOpen,

  persist = true,
  options = {
    showBackBtn: true,
  },
  isProcessing = false,
  stepProps,
}: MinimalMultiStepProps<T>) {
  const [internalOpen] = useState(false);
  const isOpen = controlledOpen ?? internalOpen;

  const [currentStep, setCurrentStep] = useState(0);
  const [direction, setDirection] = useState(0);

  const variants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 20 : -20,
      opacity: 0,
      filter: "blur(4px)",
    }),
    center: {
      zIndex: 1,
      x: 0,
      opacity: 1,
      filter: "blur(0px)",
    },
    exit: (direction: number) => ({
      zIndex: 0,
      x: direction < 0 ? 20 : -20,
      opacity: 0,
      filter: "blur(4px)",
    }),
  };

  // Conditional hook usage is tricky. We'll use both and pick one based on persist prop.
  // However, hooks cannot be conditional. So we always run both or refactor.
  // Better approach: Use a custom hook or just use persisted state but clear it if persist is false?
  // Or just use a wrapper.
  // Let's use a simple state for non-persisted.

  const persistedState = usePersistedState<T>(initialValues, {
    storageKey: `${storageKey}_values`,
  });

  const simpleState = useState<T>(initialValues);

  const values = persist ? persistedState.state : simpleState[0];
  const setValues = persist ? persistedState.setState : simpleState[1];

  // Reset state on close if not persisting
  useEffect(() => {
    if (!persist && !isOpen) {
      const timer = setTimeout(() => {
        setCurrentStep(0);
        setValues(initialValues);
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [isOpen, persist, initialValues, setValues]);

  const { status, setStatus } = useStatus({
    namespace: storageKey,
  });

  const next = useCallback(
    async (providedValues?: T) => {
      if (currentStep < steps.length - 1) {
        setDirection(1);
        setCurrentStep((prev) => prev + 1);
      } else {
        if (onComplete) {
          await onComplete(providedValues || values);
        }
        // Optional: close dialog on complete?
        // setOpen(false);
      }
    },
    [currentStep, steps.length, onComplete, values],
  );

  const prev = useCallback(() => {
    if (currentStep > 0) {
      setDirection(-1);
      setCurrentStep((prev) => prev - 1);
    }
  }, [currentStep]);

  const goToStep = useCallback(
    (step: number) => {
      if (step >= 0 && step < steps.length) {
        setDirection(step > currentStep ? 1 : -1);
        setCurrentStep(step);
      }
    },
    [steps.length, currentStep],
  );

  const CurrentStepComponent = steps[currentStep];

  return (
    <>
      {/* Show MinimalMultistep header if icon or title or description is provided */}
      {icon ||
        title ||
        (description && (
          <div className="mb-2 flex flex-col items-center gap-2">
            <div className="flex items-center gap-2">
              {icon && (
                <Icon
                  className="icon h-6 w-6"
                  color="currentColor"
                  variant={"Bulk"}
                />
              )}
              <div className="divider"></div>
              {title && <h2 className="text-lg font-semibold">{title}</h2>}
            </div>
            {description && (
              <p className="text-muted-foreground text-sm">{description}</p>
            )}
          </div>
        ))}
      <div className="space-y-4 p-1">
        {/* Progress Indicator (Optional) */}
        <div className="mb-4 flex items-center gap-2">
          {steps.map((_, idx) => (
            <div
              key={idx}
              className={`h-1 flex-1 rounded-full transition-colors ${
                idx <= currentStep ? "bg-primary" : "bg-muted"
              }`}
            />
          ))}
        </div>

        {options?.showBackBtn && currentStep > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={prev}
            className="text-muted-foreground hover:text-foreground mb-2 -ml-2"
            disabled={isProcessing}
          >
            <ArrowLeft
              size={16}
              className=""
              color="currentColor"
              variant="Bulk"
            />
            Back
          </Button>
        )}

        <AnimatePresence mode="wait" custom={direction}>
          <motion.div
            key={currentStep}
            custom={direction}
            variants={variants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{
              x: { type: "spring", stiffness: 300, damping: 30 },
              opacity: { duration: 0.2 },
            }}
          >
            <CurrentStepComponent
              {...stepProps}
              values={values}
              setValues={setValues}
              next={next}
              prev={prev}
              isFirst={currentStep === 0}
              isLast={currentStep === steps.length - 1}
              status={status}
              setStatus={setStatus as (key: string, item: unknown) => void}
              goToStep={goToStep}
              isProcessing={isProcessing}
              stepInfo={{
                currentStep,
                totalSteps: steps.length,
              }}
            />
          </motion.div>
        </AnimatePresence>
      </div>
    </>
  );
}
