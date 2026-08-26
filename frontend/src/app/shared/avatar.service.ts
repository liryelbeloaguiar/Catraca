import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

@Injectable({ providedIn: "root" })
export class AvatarService {
  constructor(private readonly http: HttpClient) {}

  load(userId: string): Observable<Blob> {
    return this.http.get(`/api/v1/profile/avatar/${userId}`, {
      responseType: "blob",
    });
  }

  upload(file: Blob): Observable<void> {
    const form = new FormData();
    form.append("file", file, "avatar.jpg");
    return this.http.post<void>("/api/v1/profile/avatar", form);
  }
}
