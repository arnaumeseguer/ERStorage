import { BreadcrumbItem, FolderContentsResponse } from './models';

export function breadcrumbTrail(contents: FolderContentsResponse): BreadcrumbItem[] {
  const { folder, breadcrumbs } = contents;
  if (folder && breadcrumbs.at(-1)?.id !== folder.id) {
    return [...breadcrumbs, { id: folder.id, name: folder.name }];
  }
  return breadcrumbs;
}