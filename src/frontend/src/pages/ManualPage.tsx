/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import useJson from "../hooks/useJson";
import useText from "../hooks/useText";
import MarkdownContent from "../components/markdown/MarkdownContent";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from "../components/ui/sidebar";
import type { SessionPageProps } from "../types/app";

interface ChapterSummary {
  slug: string;
  title: string;
}

interface ChaptersResponse {
  chapters: ChapterSummary[];
}

export default function ManualPage({ sessionState }: SessionPageProps) {
  void sessionState;
  const { chapter } = useParams<{ chapter?: string }>();
  const navigate = useNavigate();
  const contentRef = useRef<HTMLDivElement>(null);

  const chaptersState = useJson<ChaptersResponse>("/api/manual/chapters");
  const chapters = chaptersState.data?.chapters ?? [];

  const firstSlug = chapters[0]?.slug ?? null;
  const activeSlug = chapter ?? firstSlug;

  const contentState = useText(
    activeSlug ? `/api/manual/content/${activeSlug}` : "",
  );

  useEffect(() => {
    if (!chapter && firstSlug) {
      navigate(`/manual/${firstSlug}`, { replace: true });
    }
  }, [chapter, firstSlug, navigate]);

  useEffect(() => {
    contentRef.current?.scrollTo({ top: 0 });
  }, [activeSlug]);

  return (
    <SidebarProvider className="min-h-0 flex-1">
      <Sidebar collapsible="none">
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>Manual</SidebarGroupLabel>
            <SidebarMenu>
              {chaptersState.loading && (
                <SidebarMenuItem>
                  <span className="px-2 text-sm text-muted-foreground">
                    Loading...
                  </span>
                </SidebarMenuItem>
              )}
              {chapters.map((ch) => (
                <SidebarMenuItem key={ch.slug}>
                  <SidebarMenuButton
                    isActive={ch.slug === activeSlug}
                    onClick={() => navigate(`/manual/${ch.slug}`)}
                  >
                    {ch.title}
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
        </SidebarContent>
      </Sidebar>

      <div ref={contentRef} className="flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-3xl px-6 py-8">
          {contentState.loading && (
            <div className="text-sm text-muted-foreground">Loading...</div>
          )}
          {contentState.error && (
            <div className="text-sm text-destructive">{contentState.error}</div>
          )}
          {!contentState.loading &&
            !contentState.error &&
            contentState.data && (
              <MarkdownContent>{contentState.data}</MarkdownContent>
            )}
        </div>
      </div>
    </SidebarProvider>
  );
}
