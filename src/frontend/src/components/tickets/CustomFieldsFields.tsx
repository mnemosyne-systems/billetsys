/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { CustomFieldView } from "../../types/domain/customFields";
import { EMPTY_CUSTOM_FIELD_SELECT } from "../../utils/customFields";
import { Field, FieldDescription, FieldLabel } from "../ui/field";
import { Input } from "../ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";

interface CustomFieldsFieldsProps {
  fields?: CustomFieldView[];
  values: Record<string, string>;
  onChange: (fieldId: string, value: string) => void;
  editable?: boolean;
  enforceRequired?: boolean;
}

export default function CustomFieldsFields({
  fields,
  values,
  onChange,
  editable = false,
  enforceRequired = true,
}: CustomFieldsFieldsProps) {
  if (!fields || fields.length === 0) {
    return null;
  }

  return (
    <>
      {fields.map((field) => {
        const key = String(field.id);
        const value = values[key] ?? "";
        const fieldEditable = editable && field.active !== false;
        const inputId = `custom-field-${key}`;
        const helpId = field.description
          ? `custom-field-${key}-description`
          : undefined;
        return (
          <Field key={key}>
            <FieldLabel htmlFor={fieldEditable ? inputId : undefined}>
              {field.name}
              {field.required && (
                <>
                  <span aria-hidden="true" className="text-destructive">
                    *
                  </span>
                  <span className="sr-only"> (required)</span>
                </>
              )}
            </FieldLabel>
            {field.description && (
              <FieldDescription id={helpId}>
                {field.description}
              </FieldDescription>
            )}
            {!fieldEditable ? (
              <Input id={inputId} value={value || "-"} readOnly />
            ) : field.fieldType === "SELECT" ? (
              <Select
                value={value || undefined}
                onValueChange={(nextValue) =>
                  onChange(
                    key,
                    nextValue === EMPTY_CUSTOM_FIELD_SELECT ? "" : nextValue,
                  )
                }
              >
                <SelectTrigger
                  id={inputId}
                  className="w-full"
                  aria-describedby={helpId}
                  aria-required={field.required}
                >
                  <SelectValue placeholder="Select an option" />
                </SelectTrigger>
                <SelectContent>
                  {!field.required && (
                    <SelectItem value={EMPTY_CUSTOM_FIELD_SELECT}>-</SelectItem>
                  )}
                  {(field.options || []).map((option) => (
                    <SelectItem key={option} value={option}>
                      {option}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <Input
                id={inputId}
                type={
                  field.fieldType === "NUMBER"
                    ? "number"
                    : field.fieldType === "DATE"
                      ? "date"
                      : "text"
                }
                step={field.fieldType === "NUMBER" ? "any" : undefined}
                inputMode={field.fieldType === "NUMBER" ? "decimal" : undefined}
                value={value}
                onChange={(event) => onChange(key, event.target.value)}
                aria-describedby={helpId}
                required={enforceRequired && field.required}
              />
            )}
          </Field>
        );
      })}
    </>
  );
}
