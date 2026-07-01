#!/usr/bin/env python3

from pathlib import Path
import re

BASE_DIR = Path(__file__).parent

ENV_FILE = BASE_DIR / ".env-keycloak"
TEMPLATE_FILE = BASE_DIR / "keycloak-realm.template.json"
OUTPUT_FILE = BASE_DIR / "keycloak-realm.json"


def load_env_file(env_path):
    values = {}

    for line in Path(env_path).read_text().splitlines():
        line = line.strip()

        # Skip comments and empty lines
        if not line or line.startswith("#"):
            continue

        key, value = line.split("=", 1)

        # Remove surrounding quotes
        value = value.strip().strip('"').strip("'")

        values[key.strip()] = value

    return values


def replace_placeholders(template, values):
    pattern = re.compile(r"\$\{([A-Z0-9_]+)\}")

    def replacer(match):
        key = match.group(1)
        return values.get(key, match.group(0))

    return pattern.sub(replacer, template)


def main():
    env_values = load_env_file(ENV_FILE)

    template_content = Path(TEMPLATE_FILE).read_text()

    output_content = replace_placeholders(
        template_content,
        env_values
    )

    Path(OUTPUT_FILE).write_text(output_content)

    print(f"Generated {OUTPUT_FILE} successfully.")


if __name__ == "__main__":
    main()