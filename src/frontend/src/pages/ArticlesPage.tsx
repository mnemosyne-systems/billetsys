/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { ArticleRecord, CollectionResponse } from "../types/domain";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";

export default function ArticlesPage({ sessionState }: SessionPageProps) {
  const articlesState =
    useJson<CollectionResponse<ArticleRecord>>("/api/articles");

  return (
    <Card className="w-full max-w-5xl mx-auto mt-4">
      <CardHeader className="flex flex-row items-center justify-between pb-6">
        <CardTitle className="text-2xl font-bold">Articles</CardTitle>
        <div>
          {articlesState.data?.canCreate && (
            <Button asChild>
              <SmartLink href={articlesState.data.createPath}>Create</SmartLink>
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent>
        <DataState
          state={articlesState}
          emptyMessage="No articles are available yet."
          signInHref={sessionState.data?.homePath || "/login"}
        >
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Tags</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(articlesState.data?.items || []).map((article) => (
                  <TableRow key={article.id}>
                    <TableCell className="font-medium">
                      <Link
                        className="text-primary hover:underline"
                        to={`/articles/${article.id}`}
                      >
                        {article.title}
                      </Link>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {article.tags || "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </DataState>
      </CardContent>
    </Card>
  );
}
