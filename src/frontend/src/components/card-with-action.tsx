import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "./ui/card";
import { Button } from "./ui/button";

export default function CardDemo() {
  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Card Title</CardTitle>
        <CardDescription>
          This is where the card description is.
        </CardDescription>
        <CardAction>
          <Button variant="secondary" size="sm">
            Sign Up
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        <p className="text-sm">
          This is the card content. Other content is added here.
        </p>
      </CardContent>
    </Card>
  );
}
