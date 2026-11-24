import {
  LucideArchive,
  LucideFile,
  LucideFileText,
  LucideFilm,
  LucideImage,
  LucideMusic,
} from '@lucide/angular';

export function fileIcon(contentType: string) {
  if (contentType.startsWith('image/')) {
    return LucideImage;
  }
  if (contentType.startsWith('video/')) {
    return LucideFilm;
  }
  if (contentType.startsWith('audio/')) {
    return LucideMusic;
  }
  if (contentType === 'application/pdf' || contentType.startsWith('text/')) {
    return LucideFileText;
  }
  if (/zip|rar|7z|tar|gzip/.test(contentType)) {
    return LucideArchive;
  }
  return LucideFile;
}