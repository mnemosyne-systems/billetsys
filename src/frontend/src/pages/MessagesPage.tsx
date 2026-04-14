/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, MessageReference } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

export default function MessagesPage(props: SessionPageProps) {
  void props;
  const messageState =
    useJson<CollectionResponse<MessageReference>>("/api/messages");
  const messages = messageState.data;

  return (
    <section className="w-full mt-4">
      <div className="flex flex-row items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {messages?.title || "Messages"}
        </h2>
        <div>
          {messages?.createPath && (
            <Button asChild>
              <SmartLink href={messages.createPath}>New message</SmartLink>
            </Button>
          )}
        </div>
      </div>

      <DataState state={messageState} emptyMessage="No messages are available.">
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {(messages?.items || []).map((message: MessageReference) => (
            <Card
              key={message.id}
              className="hover:shadow-md transition-shadow"
            >
              <CardHeader className="flex flex-col h-full justify-between gap-4">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold leading-none tracking-tight">
                      {message.ticketName || "No ticket"}
                    </h3>
                    <Badge
                      variant="secondary"
                      className="whitespace-nowrap font-normal"
                    >
                      {message.date || "No date"}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground mt-3 line-clamp-2">
                    {message.preview}
                  </p>
                  <p className="text-sm text-muted-foreground mt-2">
                    {message.authorName || "Unknown author"} •{" "}
                    {message.attachmentCount} attachment(s)
                  </p>
                </div>
                <div className="flex justify-end">
                  <SmartLink
                    className="text-sm font-medium text-primary hover:underline"
                    href={message.editPath}
                  >
                    Edit
                  </SmartLink>
                </div>
              </CardHeader>
            </Card>
          ))}
        </div>
      </DataState>
    </section>
  );
}
