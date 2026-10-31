/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import assert from "node:assert/strict";
import test from "node:test";
import { createReferenceCodec } from "refmint/codec";
import {
  formatTicketReference,
  ticketReferencePath,
} from "../src/utils/ticketReferences.ts";

const codec = createReferenceCodec({
  prefix: "btk",
  kind: "uint64",
  version: 1,
});

test("existing ticket IDs round trip without an allocation or mapping table", () => {
  for (const id of [
    1,
    42,
    Number.MAX_SAFE_INTEGER,
    "9007199254740993",
    "9223372036854775807",
  ]) {
    const reference = formatTicketReference(id);
    assert.equal(typeof reference, "string");
    assert.equal(ticketReferencePath(reference, "user"), `/user/tickets/${id}`);
    assert.equal(
      ticketReferencePath(codec.encode(BigInt(id)), "user"),
      `/user/tickets/${id}`,
    );
  }
  // A stable vector catches accidental changes to the namespace or wire format.
  assert.equal(formatTicketReference(42), "btk_1pqp2-cy59-jf");
});

test("a reference opens the existing ticket route for each ticket-viewing role", () => {
  const reference = formatTicketReference(42);
  for (const [role, expected] of [
    ["support", "/support/tickets/42"],
    ["superuser", "/superuser/tickets/42"],
    ["tam", "/user/tickets/42"],
    ["user", "/user/tickets/42"],
  ]) {
    assert.equal(ticketReferencePath(reference, role), expected);
  }
  for (const role of [undefined, "", "admin", "external", "administrator"]) {
    assert.equal(ticketReferencePath(reference, role), null);
  }
});

test("accepted presentation changes preserve the ID", () => {
  const raw = codec.encode(42n);
  for (const input of [
    raw,
    raw.toUpperCase(),
    codec.format(raw),
    `  ${raw}  `,
  ]) {
    assert.equal(ticketReferencePath(input, "support"), "/support/tickets/42");
  }
});

test("manual single-symbol errors never resolve to a different ticket", () => {
  const raw = codec.encode(12345n);
  const symbols = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
  for (let i = raw.indexOf("1") + 1; i < raw.length; i++) {
    for (const replacement of symbols) {
      if (replacement === raw[i]) continue;
      const typo = raw.slice(0, i) + replacement + raw.slice(i + 1);
      assert.equal(ticketReferencePath(typo, "support"), null, typo);
    }
  }
});

test("foreign references and IDs outside the database domain are rejected", () => {
  const other = createReferenceCodec({ prefix: "other", kind: "uint64" });
  const uuid = createReferenceCodec({ prefix: "btk", kind: "uuid" });
  for (const input of [
    other.encode(42n),
    uuid.encode("00000000-0000-0000-0000-00000000002a"),
    codec.encode(0n),
    codec.encode(1n << 63n),
  ]) {
    assert.equal(ticketReferencePath(input, "user"), null);
  }
});

test("malformed input cannot become a route or an uncaught exception", () => {
  for (const input of [
    null,
    undefined,
    42,
    {},
    [],
    "",
    "42",
    "../../tickets/42",
    "https://example.com",
    "x".repeat(10000),
    "ｂｔｋ",
    "btk_1💥",
    " btk_1q ",
  ]) {
    assert.equal(ticketReferencePath(input, "user"), null);
  }
});

test("unsafe JSON numbers are never advertised as a reference for another ID", () => {
  for (const id of [
    Number.MAX_SAFE_INTEGER + 1,
    Infinity,
    NaN,
    1.5,
    0,
    -1,
    "0",
    "01",
    "-1",
    "1.0",
    "1e3",
    " 42 ",
    "9223372036854775808",
    "9".repeat(10000),
  ]) {
    assert.equal(formatTicketReference(id), null);
  }
});

// Exercise the actual components through the project's existing Vite compiler,
// without adding another test framework or changing the app's authentication.
for (const enabled of [false, true]) {
  test(`reference controls respect the build flag (${enabled}) and roles`, async () => {
    const { createServer } = await import("vite");
    const server = await createServer({
      server: { middlewareMode: true, watch: null, hmr: false },
      optimizeDeps: { noDiscovery: true, include: [] },
      appType: "custom",
      define: {
        "import.meta.env.VITE_TICKET_REFERENCES": JSON.stringify(
          String(enabled),
        ),
      },
      plugins: [
        {
          name: "ticket-reference-test-renderer",
          resolveId(id) {
            if (id === "virtual:ticket-reference-test") return "\0" + id;
          },
          load(id) {
            if (id !== "\0virtual:ticket-reference-test") return;
            return `
            import { createElement as h } from 'react';
            import { renderToStaticMarkup } from 'react-dom/server';
            import { MemoryRouter } from 'react-router-dom';
            import TicketReference from '/src/components/tickets/TicketReference.tsx';
            import TicketReferenceLookup from '/src/components/tickets/TicketReferenceLookup.tsx';
            export function display(ticketId) {
              return renderToStaticMarkup(h(TicketReference, { ticketId }));
            }
            export function lookup(role) {
              return renderToStaticMarkup(h(MemoryRouter, null, h(TicketReferenceLookup, { role })));
            }
          `;
          },
        },
      ],
    });
    try {
      const renderer = await server.ssrLoadModule(
        "virtual:ticket-reference-test",
      );
      const display = renderer.display(42);
      if (enabled) {
        assert.ok(display.includes(formatTicketReference(42)));
        assert.match(display, /readonly/i);
        assert.match(display, /type="button"/); // Copy must not submit the edit form.
      } else assert.equal(display, "");
      assert.equal(renderer.display(Number.MAX_SAFE_INTEGER + 1), "");
      for (const role of ["support", "superuser", "tam", "user"]) {
        const lookup = renderer.lookup(role);
        if (enabled) assert.match(lookup, /Find ticket by reference/);
        else assert.equal(lookup, "");
      }
      for (const role of [undefined, "admin", "external", "unknown"]) {
        assert.equal(renderer.lookup(role), "");
      }
    } finally {
      await server.close();
    }
  });
}
