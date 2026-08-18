import { DefaultUrlSerializer, UrlTree } from '@angular/router';

export class ReadableUrlSerializer extends DefaultUrlSerializer {
  private static readonly ENCODED_BRACKET_PATTERN = /%5[bd]/gi;
  private static readonly BRACKETS_BY_ENCODING: Record<string, string> = { '%5b': '[', '%5d': ']' };

  public override serialize(tree: UrlTree): string {
    const url = super.serialize(tree);
    const queryStart = url.indexOf('?');
    if (queryStart === -1) return url;

    const fragmentStart = url.indexOf('#', queryStart);
    const queryEnd = fragmentStart === -1 ? url.length : fragmentStart;

    const query = url
      .slice(queryStart, queryEnd)
      .replaceAll(
        ReadableUrlSerializer.ENCODED_BRACKET_PATTERN,
        (encoded) => ReadableUrlSerializer.BRACKETS_BY_ENCODING[encoded.toLowerCase()],
      );

    return `${url.slice(0, queryStart)}${query}${fragmentStart === -1 ? '' : url.slice(fragmentStart)}`;
  }
}
