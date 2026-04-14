/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { ArticleRecord, CollectionResponse } from "../types/domain";
import { Button } from "../components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";

export default function ArticlesPage(props: SessionPageProps) {
  void props;
  const articlesState =
    useJson<CollectionResponse<ArticleRecord>>("/api/articles");

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title="Articles"
        actions={
          articlesState.data?.canCreate ? (
            <Button asChild>
              <SmartLink href={articlesState.data.createPath}>Create</SmartLink>
            </Button>
          ) : null
        }
      />

      <div className="w-full">
        <DataState
          state={articlesState}
          emptyMessage="No articles are available yet."
        >
          <div className="max-w-full overflow-x-auto">
            <Table className="text-base">
              <TableHeader>
                <TableRow className="bg-muted/50 hover:bg-muted/50">
                  <TableHead className="min-w-[240px]">Title</TableHead>
                  <TableHead className="min-w-[180px]">Tags</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(articlesState.data?.items || []).map((article) => (
                  <TableRow key={article.id}>
                    <TableCell className="py-3 px-4 font-medium">
                      <SmartLink
                        className="font-semibold text-primary hover:underline"
                        href={`/articles/${article.id}`}
                      >
                        {article.title}
                      </SmartLink>
                    </TableCell>
                    <TableCell className="py-3 px-4 text-muted-foreground">
                      {article.tags || "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </DataState>
      </div>
    </div>
  );
}
