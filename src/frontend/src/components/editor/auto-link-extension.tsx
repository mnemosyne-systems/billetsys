import { $isCodeNode } from "@lexical/code";
import {
  AutoLinkExtension as LexicalAutoLinkExtension,
  createLinkMatcherWithRegExp,
} from "@lexical/link";
import { configExtension } from "lexical";

const URL_REGEX =
  /((https?:\/\/(www\.)?)|(www\.))[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)(?<![-.+():%])/;

const EMAIL_REGEX =
  /(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))/;

export const AutoLinkExtension = configExtension(LexicalAutoLinkExtension, {
  excludeParents: [$isCodeNode],
  matchers: [
    createLinkMatcherWithRegExp(URL_REGEX, (text) => {
      return text.startsWith("http") ? text : `https://${text}`;
    }),
    createLinkMatcherWithRegExp(EMAIL_REGEX, (text) => {
      return `mailto:${text}`;
    }),
  ],
});
