import { DefaultUrlSerializer, UrlTree } from '@angular/router';

/**
 * Keeps `[` and `]` literal in the query string so the graph's `?range=[9.0],[9.4,10.0)` links stay
 * readable and hand-editable, instead of `?range=%5B9.0%5D,%5B9.4,10.0%29`.
 *
 * Requires `server.tomcat.relaxed-query-chars=[,]` on the server; Tomcat returns 400 Bad Request for
 * these characters by default. See docs/ARCHITECTURE.md, "Decisions and why", for the full trade-off.
 */
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
