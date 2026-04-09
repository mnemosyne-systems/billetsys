import { CalendarIcon } from "lucide-react";

import { INSERT_DATETIME_COMMAND } from "@/components/editor/date-time-extension";
import { ComponentPickerOption } from "@/components/editor/component-picker-option";

export function DateTimePickerPlugin() {
  return new ComponentPickerOption("Date", {
    icon: <CalendarIcon className="size-4" />,
    keywords: ["date", "calendar", "time", "today"],
    onSelect: (_, editor) => {
      const dateTime = new Date();
      dateTime.setHours(0, 0, 0, 0); // Set time to midnight
      editor.dispatchCommand(INSERT_DATETIME_COMMAND, { dateTime });
    },
  });
}
