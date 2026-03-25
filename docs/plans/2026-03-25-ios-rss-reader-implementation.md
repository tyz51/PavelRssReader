# iOS RSS Reader Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a full-parity iOS SwiftUI app at `~/Documents/Ios_Dev/PavelRssReader-iOS/` that mirrors the Android RSS reader — 6 screens, MVVM + Clean Architecture, SwiftData persistence, URLSession networking, native RSS/Atom parser, HTML article renderer.

**Architecture:** MVVM + Clean Architecture with three layers: Domain (pure Swift structs/protocols/use-cases), Data (SwiftData, URLSession, SwiftSoup), Presentation (@Observable ViewModels + SwiftUI Views). Dependencies injected via initializers and `@Environment`.

**Tech Stack:** Swift 6, SwiftUI, SwiftData, URLSession, Apple XMLParser, SwiftSoup (via SPM), XCTest, iOS 17+ deployment target.

---

## Task 1: Project Scaffold

**Files:**
- Create: `~/Documents/Ios_Dev/PavelRssReader-iOS/project.yml`
- Create: `~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOS/Info.plist`

**Step 1: Install xcodegen**

```bash
brew install xcodegen
```
Expected: installs successfully (or "already installed").

**Step 2: Create the project directory**

```bash
mkdir -p ~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOS
mkdir -p ~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOSTests
```

**Step 3: Create `project.yml`**

Save to `~/Documents/Ios_Dev/PavelRssReader-iOS/project.yml`:

```yaml
name: PavelRssReader-iOS
options:
  bundleIdPrefix: com.pavel
  deploymentTarget:
    iOS: "17.0"
  createIntermediateGroups: true

packages:
  SwiftSoup:
    url: https://github.com/scinfu/SwiftSoup
    from: "2.7.6"

targets:
  PavelRssReader-iOS:
    type: application
    platform: iOS
    deploymentTarget: "17.0"
    sources: [PavelRssReader-iOS]
    dependencies:
      - package: SwiftSoup
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.pavel.pavelrssreader.ios
        SWIFT_VERSION: "5.9"
        INFOPLIST_FILE: PavelRssReader-iOS/Info.plist

  PavelRssReader-iOSTests:
    type: bundle.unit-test
    platform: iOS
    deploymentTarget: "17.0"
    sources: [PavelRssReader-iOSTests]
    dependencies:
      - target: PavelRssReader-iOS
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.pavel.pavelrssreader.ios.tests
```

**Step 4: Create `Info.plist`**

Save to `~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOS/Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key>
  <string>PavelRssReader</string>
  <key>CFBundleDisplayName</key>
  <string>RSS Reader</string>
  <key>UILaunchScreen</key>
  <dict/>
  <key>NSAppTransportSecurity</key>
  <dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
  </dict>
</dict>
</plist>
```

**Step 5: Generate Xcode project**

```bash
cd ~/Documents/Ios_Dev/PavelRssReader-iOS && xcodegen generate
```
Expected: `✅ Done` — creates `PavelRssReader-iOS.xcodeproj`.

**Step 6: Create placeholder app entry point**

Save to `~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOS/PavelRssReaderApp.swift`:

```swift
import SwiftUI

@main
struct PavelRssReaderApp: App {
    var body: some Scene {
        WindowGroup {
            Text("Hello iOS")
        }
    }
}
```

**Step 7: Verify project builds**

```bash
cd ~/Documents/Ios_Dev/PavelRssReader-iOS
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -5
```
Expected: `** BUILD SUCCEEDED **`

**Step 8: Init git and commit**

```bash
cd ~/Documents/Ios_Dev/PavelRssReader-iOS
git init
echo ".DS_Store\n*.xcuserstate\nxcuserdata/\nDerivedData/" > .gitignore
git add .
git commit -m "chore: initial iOS project scaffold"
```

---

## Task 2: Domain Layer — Models, Protocols, Use Cases

**Files:**
- Create: `PavelRssReader-iOS/domain/model/Article.swift`
- Create: `PavelRssReader-iOS/domain/model/Feed.swift`
- Create: `PavelRssReader-iOS/domain/model/ContentBlock.swift`
- Create: `PavelRssReader-iOS/domain/model/FeedUnreadCount.swift`
- Create: `PavelRssReader-iOS/domain/model/ThemePreference.swift`
- Create: `PavelRssReader-iOS/domain/model/AppError.swift`
- Create: `PavelRssReader-iOS/domain/repository/RssRepositoryProtocol.swift`
- Create: `PavelRssReader-iOS/domain/repository/SettingsRepositoryProtocol.swift`
- Create: `PavelRssReader-iOS/domain/usecase/` (10 files)

**Step 1: Create domain models**

`domain/model/Article.swift`:
```swift
import Foundation

struct Article: Identifiable, Equatable {
    let id: UUID
    let feedId: UUID
    let guid: String
    let title: String
    let link: String
    let description: String
    let publishedAt: Date
    let fetchedAt: Date
    var isRead: Bool
    var isFavorite: Bool
    let imageUrl: String?
    let sourceName: String
}
```

`domain/model/Feed.swift`:
```swift
import Foundation

struct Feed: Identifiable, Equatable {
    let id: UUID
    let url: String
    let title: String
    let addedAt: Date
}
```

`domain/model/ContentBlock.swift`:
```swift
enum ContentBlock {
    case heading(level: Int, text: String)
    case paragraph(spans: [TextSpan])
    case image(url: String, caption: String?)
    case quote(text: String)
}

enum TextSpan {
    case plain(String)
    case bold(String)
    case italic(String)
    case link(text: String, url: String)
}
```

`domain/model/FeedUnreadCount.swift`:
```swift
import Foundation

struct FeedUnreadCount: Equatable {
    let feedId: UUID
    let count: Int
}
```

`domain/model/ThemePreference.swift`:
```swift
enum ThemePreference: String, CaseIterable {
    case system = "SYSTEM"
    case light  = "LIGHT"
    case dark   = "DARK"
}
```

`domain/model/AppError.swift`:
```swift
enum AppError: Error, LocalizedError {
    case networkError(String)
    case parseError(String)
    case databaseError(String)

    var errorDescription: String? {
        switch self {
        case .networkError(let m):  return "Network error: \(m)"
        case .parseError(let m):    return "Parse error: \(m)"
        case .databaseError(let m): return "Database error: \(m)"
        }
    }
}
```

**Step 2: Create repository protocols**

`domain/repository/RssRepositoryProtocol.swift`:
```swift
import Foundation

protocol RssRepositoryProtocol {
    func getAllArticles() async throws -> [Article]
    func getFavouriteArticles() async throws -> [Article]
    func getAllFeeds() async throws -> [Feed]
    func getUnreadCountsPerFeed() async throws -> [FeedUnreadCount]
    func addFeed(url: String) async throws
    func deleteFeed(id: UUID) async throws
    func refreshFeeds() async throws
    func setFavourite(id: UUID, isFavorite: Bool) async throws
    func markAsRead(id: UUID) async throws
    func markAsUnread(id: UUID) async throws
    func getNextUnreadArticle(after articleId: UUID, feedId: UUID?) async throws -> Article?
}
```

`domain/repository/SettingsRepositoryProtocol.swift`:
```swift
protocol SettingsRepositoryProtocol: AnyObject {
    var titleFontSize: Float { get }
    var bodyFontSize: Float { get }
    var themePreference: ThemePreference { get }
    func setTitleFontSize(_ size: Float)
    func setBodyFontSize(_ size: Float)
    func setThemePreference(_ pref: ThemePreference)
}
```

**Step 3: Create use cases**

`domain/usecase/GetArticlesUseCase.swift`:
```swift
struct GetArticlesUseCase {
    let repository: RssRepositoryProtocol
    func execute() async throws -> [Article] {
        try await repository.getAllArticles()
    }
}
```

`domain/usecase/GetFavouritesUseCase.swift`:
```swift
struct GetFavouritesUseCase {
    let repository: RssRepositoryProtocol
    func execute() async throws -> [Article] {
        try await repository.getFavouriteArticles()
    }
}
```

`domain/usecase/GetFeedsUseCase.swift`:
```swift
struct GetFeedsUseCase {
    let repository: RssRepositoryProtocol
    func execute() async throws -> [Feed] {
        try await repository.getAllFeeds()
    }
}
```

`domain/usecase/GetUnreadCountsPerFeedUseCase.swift`:
```swift
struct GetUnreadCountsPerFeedUseCase {
    let repository: RssRepositoryProtocol
    func execute() async throws -> [FeedUnreadCount] {
        try await repository.getUnreadCountsPerFeed()
    }
}
```

`domain/usecase/AddFeedUseCase.swift`:
```swift
struct AddFeedUseCase {
    let repository: RssRepositoryProtocol
    func execute(url: String) async throws {
        try await repository.addFeed(url: url)
    }
}
```

`domain/usecase/DeleteFeedUseCase.swift`:
```swift
import Foundation
struct DeleteFeedUseCase {
    let repository: RssRepositoryProtocol
    func execute(id: UUID) async throws {
        try await repository.deleteFeed(id: id)
    }
}
```

`domain/usecase/RefreshFeedsUseCase.swift`:
```swift
struct RefreshFeedsUseCase {
    let repository: RssRepositoryProtocol
    func execute() async throws {
        try await repository.refreshFeeds()
    }
}
```

`domain/usecase/ToggleFavouriteUseCase.swift`:
```swift
import Foundation
struct ToggleFavouriteUseCase {
    let repository: RssRepositoryProtocol
    func execute(id: UUID, isFavorite: Bool) async throws {
        try await repository.setFavourite(id: id, isFavorite: isFavorite)
    }
}
```

`domain/usecase/MarkAsReadUseCase.swift`:
```swift
import Foundation
struct MarkAsReadUseCase {
    let repository: RssRepositoryProtocol
    func execute(id: UUID) async throws {
        try await repository.markAsRead(id: id)
    }
}
```

`domain/usecase/UnmarkAsReadUseCase.swift`:
```swift
import Foundation
struct UnmarkAsReadUseCase {
    let repository: RssRepositoryProtocol
    func execute(id: UUID) async throws {
        try await repository.markAsUnread(id: id)
    }
}
```

**Step 4: Verify build**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
```
Expected: `** BUILD SUCCEEDED **`

**Step 5: Commit**

```bash
git add PavelRssReader-iOS/domain
git commit -m "feat: add domain layer — models, repository protocols, use cases"
```

---

## Task 3: SwiftData Models + Mappers

**Files:**
- Create: `PavelRssReader-iOS/data/db/FeedModel.swift`
- Create: `PavelRssReader-iOS/data/db/ArticleModel.swift`
- Create: `PavelRssReader-iOS/data/db/Mappers.swift`

**Step 1: Create SwiftData models**

`data/db/FeedModel.swift`:
```swift
import SwiftData
import Foundation

@Model
final class FeedModel {
    @Attribute(.unique) var id: UUID
    var url: String
    var title: String
    var addedAt: Date
    @Relationship(deleteRule: .cascade) var articles: [ArticleModel] = []

    init(id: UUID = UUID(), url: String, title: String, addedAt: Date = Date()) {
        self.id = id
        self.url = url
        self.title = title
        self.addedAt = addedAt
    }
}
```

`data/db/ArticleModel.swift`:
```swift
import SwiftData
import Foundation

@Model
final class ArticleModel {
    @Attribute(.unique) var id: UUID
    var feedId: UUID
    var guid: String
    var title: String
    var link: String
    var articleDescription: String  // 'description' conflicts with Swift's CustomStringConvertible
    var publishedAt: Date
    var fetchedAt: Date
    var isRead: Bool
    var isFavorite: Bool
    var imageUrl: String?
    var sourceName: String
    var feed: FeedModel?

    init(
        id: UUID = UUID(), feedId: UUID, guid: String, title: String,
        link: String, articleDescription: String, publishedAt: Date,
        fetchedAt: Date = Date(), isRead: Bool = false, isFavorite: Bool = false,
        imageUrl: String? = nil, sourceName: String = ""
    ) {
        self.id = id; self.feedId = feedId; self.guid = guid
        self.title = title; self.link = link
        self.articleDescription = articleDescription
        self.publishedAt = publishedAt; self.fetchedAt = fetchedAt
        self.isRead = isRead; self.isFavorite = isFavorite
        self.imageUrl = imageUrl; self.sourceName = sourceName
    }
}
```

`data/db/Mappers.swift`:
```swift
extension ArticleModel {
    func toDomain() -> Article {
        Article(
            id: id, feedId: feedId, guid: guid, title: title,
            link: link, description: articleDescription,
            publishedAt: publishedAt, fetchedAt: fetchedAt,
            isRead: isRead, isFavorite: isFavorite,
            imageUrl: imageUrl, sourceName: sourceName
        )
    }
}

extension FeedModel {
    func toDomain() -> Feed {
        Feed(id: id, url: url, title: title, addedAt: addedAt)
    }
}
```

**Step 2: Build and commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/data/db
git commit -m "feat: add SwiftData models and domain mappers"
```

---

## Task 4: RssParser (TDD)

**Files:**
- Create: `PavelRssReader-iOS/data/network/RssParser.swift`
- Create: `PavelRssReader-iOSTests/RssParserTests.swift`

**Step 1: Write failing tests**

`PavelRssReader-iOSTests/RssParserTests.swift`:
```swift
import XCTest
@testable import PavelRssReader_iOS

final class RssParserTests: XCTestCase {

    let rss2XML = """
    <?xml version="1.0"?>
    <rss version="2.0">
      <channel>
        <title>Test Feed</title>
        <item>
          <title>Article One</title>
          <link>https://example.com/1</link>
          <description>Body text</description>
          <guid>guid-1</guid>
          <pubDate>Mon, 01 Jan 2024 12:00:00 +0000</pubDate>
          <media:thumbnail url="https://example.com/img.jpg"/>
        </item>
      </channel>
    </rss>
    """

    let atomXML = """
    <?xml version="1.0"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>Atom Feed</title>
      <entry>
        <title>Atom Article</title>
        <link href="https://example.com/atom/1"/>
        <summary>Atom summary</summary>
        <id>atom-guid-1</id>
        <updated>2024-01-01T12:00:00Z</updated>
      </entry>
    </feed>
    """

    func test_parseRss2_returnsFeedTitle() throws {
        let feed = try RssParser().parse(xmlString: rss2XML)
        XCTAssertEqual(feed.title, "Test Feed")
    }

    func test_parseRss2_returnsArticleTitle() throws {
        let feed = try RssParser().parse(xmlString: rss2XML)
        XCTAssertEqual(feed.items.first?.title, "Article One")
    }

    func test_parseRss2_returnsArticleLink() throws {
        let feed = try RssParser().parse(xmlString: rss2XML)
        XCTAssertEqual(feed.items.first?.link, "https://example.com/1")
    }

    func test_parseRss2_returnsImageUrl() throws {
        let feed = try RssParser().parse(xmlString: rss2XML)
        XCTAssertEqual(feed.items.first?.imageUrl, "https://example.com/img.jpg")
    }

    func test_parseAtom_returnsFeedTitle() throws {
        let feed = try RssParser().parse(xmlString: atomXML)
        XCTAssertEqual(feed.title, "Atom Feed")
    }

    func test_parseAtom_returnsArticleTitle() throws {
        let feed = try RssParser().parse(xmlString: atomXML)
        XCTAssertEqual(feed.items.first?.title, "Atom Article")
    }

    func test_parseAtom_returnsArticleLink() throws {
        let feed = try RssParser().parse(xmlString: atomXML)
        XCTAssertEqual(feed.items.first?.link, "https://example.com/atom/1")
    }
}
```

**Step 2: Run tests — expect failure**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/RssParserTests 2>&1 | grep -E "error:|FAILED|PASSED"
```
Expected: compile error — `RssParser` type not found.

**Step 3: Create `RssParser.swift`**

`data/network/RssParser.swift`:
```swift
import Foundation

struct ParsedFeed {
    let title: String
    let items: [ParsedArticle]
}

struct ParsedArticle {
    let guid: String
    let title: String
    let link: String
    let description: String
    let publishedAt: Date
    let imageUrl: String?
}

final class RssParser: NSObject, XMLParserDelegate {

    private var feedTitle = ""
    private var items: [ParsedArticle] = []
    private var currentElement = ""
    private var isInsideItem = false
    private var isAtomFeed = false

    private var itemTitle = ""
    private var itemLink = ""
    private var itemDescription = ""
    private var itemGuid = ""
    private var itemPubDate = ""
    private var itemImageUrl: String? = nil
    private var buffer = ""

    private static let rfc822: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        return f
    }()

    private static let iso8601: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    func parse(xmlString: String) throws -> ParsedFeed {
        guard let data = xmlString.data(using: .utf8) else {
            throw AppError.parseError("Invalid encoding")
        }
        feedTitle = ""; items = []; isAtomFeed = false
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.parse()
        return ParsedFeed(title: feedTitle, items: items)
    }

    // MARK: — XMLParserDelegate

    func parser(_ parser: XMLParser, didStartElement elementName: String,
                namespaceURI: String?, qualifiedName: String?,
                attributes: [String: String] = [:]) {
        currentElement = elementName
        buffer = ""
        if elementName == "feed" { isAtomFeed = true }
        if elementName == "item" || elementName == "entry" {
            isInsideItem = true
            itemTitle = ""; itemLink = ""; itemDescription = ""
            itemGuid = ""; itemPubDate = ""; itemImageUrl = nil
        }
        // RSS: <enclosure> image
        if elementName == "enclosure", let type = attributes["type"],
           type.hasPrefix("image"), let url = attributes["url"] {
            itemImageUrl = url
        }
        // media:thumbnail or media:content
        if (elementName == "media:thumbnail" || elementName == "media:content"),
           let url = attributes["url"] {
            itemImageUrl = itemImageUrl ?? url
        }
        // Atom: <link href="...">
        if isAtomFeed && elementName == "link",
           let href = attributes["href"], itemLink.isEmpty {
            itemLink = href
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        buffer += string
    }

    func parser(_ parser: XMLParser, didEndElement elementName: String,
                namespaceURI: String?, qualifiedName: String?) {
        let text = buffer.trimmingCharacters(in: .whitespacesAndNewlines)

        if !isInsideItem {
            if elementName == "title" && feedTitle.isEmpty { feedTitle = text }
        } else {
            switch elementName {
            case "title":        itemTitle = text
            case "link":         if !isAtomFeed && itemLink.isEmpty { itemLink = text }
            case "description", "summary": if itemDescription.isEmpty { itemDescription = text }
            case "content:encoded": itemDescription = text   // prefer full content
            case "guid", "id":   itemGuid = text
            case "pubDate", "updated", "published": if itemPubDate.isEmpty { itemPubDate = text }
            case "item", "entry":
                let date = Self.rfc822.date(from: itemPubDate)
                    ?? Self.iso8601.date(from: itemPubDate)
                    ?? Date()
                let guid = itemGuid.isEmpty ? itemLink : itemGuid
                items.append(ParsedArticle(
                    guid: guid, title: itemTitle, link: itemLink,
                    description: itemDescription, publishedAt: date,
                    imageUrl: itemImageUrl
                ))
                isInsideItem = false
            default: break
            }
        }
        buffer = ""
    }
}
```

**Step 4: Run tests — expect pass**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/RssParserTests 2>&1 | grep -E "Test Suite|PASSED|FAILED"
```
Expected: all 7 tests PASSED.

**Step 5: Commit**

```bash
git add PavelRssReader-iOS/data/network/RssParser.swift PavelRssReader-iOSTests/RssParserTests.swift
git commit -m "feat: add RSS 2.0 + Atom parser with tests"
```

---

## Task 5: HtmlToBlocks (TDD)

**Files:**
- Create: `PavelRssReader-iOS/data/parser/HtmlToBlocks.swift`
- Create: `PavelRssReader-iOSTests/HtmlToBlocksTests.swift`

**Step 1: Write failing tests**

`PavelRssReader-iOSTests/HtmlToBlocksTests.swift`:
```swift
import XCTest
@testable import PavelRssReader_iOS

final class HtmlToBlocksTests: XCTestCase {

    func test_heading_parsedAsHeading() throws {
        let blocks = try HtmlToBlocks.convert("<h1>My Title</h1>")
        XCTAssertEqual(blocks.first, .heading(level: 1, text: "My Title"))
    }

    func test_paragraph_parsedAsParagraph() throws {
        let blocks = try HtmlToBlocks.convert("<p>Hello world</p>")
        if case .paragraph(let spans) = blocks.first {
            XCTAssertEqual(spans, [.plain("Hello world")])
        } else {
            XCTFail("Expected paragraph block")
        }
    }

    func test_bold_parsedAsBoldSpan() throws {
        let blocks = try HtmlToBlocks.convert("<p><strong>Bold text</strong></p>")
        if case .paragraph(let spans) = blocks.first {
            XCTAssertTrue(spans.contains(.bold("Bold text")))
        } else {
            XCTFail("Expected paragraph with bold span")
        }
    }

    func test_image_parsedAsImageBlock() throws {
        let blocks = try HtmlToBlocks.convert("<img src=\"https://ex.com/img.jpg\" alt=\"caption\"/>")
        XCTAssertEqual(blocks.first, .image(url: "https://ex.com/img.jpg", caption: "caption"))
    }

    func test_blockquote_parsedAsQuote() throws {
        let blocks = try HtmlToBlocks.convert("<blockquote>Some quote</blockquote>")
        XCTAssertEqual(blocks.first, .quote(text: "Some quote"))
    }
}

extension ContentBlock: Equatable {
    public static func == (lhs: ContentBlock, rhs: ContentBlock) -> Bool {
        switch (lhs, rhs) {
        case (.heading(let l1, let t1), .heading(let l2, let t2)): return l1 == l2 && t1 == t2
        case (.paragraph(let s1), .paragraph(let s2)): return s1 == s2
        case (.image(let u1, let c1), .image(let u2, let c2)): return u1 == u2 && c1 == c2
        case (.quote(let t1), .quote(let t2)): return t1 == t2
        default: return false
        }
    }
}

extension TextSpan: Equatable {
    public static func == (lhs: TextSpan, rhs: TextSpan) -> Bool {
        switch (lhs, rhs) {
        case (.plain(let a), .plain(let b)): return a == b
        case (.bold(let a), .bold(let b)): return a == b
        case (.italic(let a), .italic(let b)): return a == b
        case (.link(let t1, let u1), .link(let t2, let u2)): return t1 == t2 && u1 == u2
        default: return false
        }
    }
}
```

**Step 2: Run tests — expect failure**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/HtmlToBlocksTests 2>&1 | grep -E "error:|FAILED|PASSED" | head -5
```
Expected: compile error — `HtmlToBlocks` not found.

**Step 3: Create `HtmlToBlocks.swift`**

`data/parser/HtmlToBlocks.swift`:
```swift
import Foundation
import SwiftSoup

enum HtmlToBlocks {
    static func convert(_ html: String) throws -> [ContentBlock] {
        let doc = try SwiftSoup.parse(html)
        let body = doc.body() ?? doc
        return try parseChildren(of: body)
    }

    private static func parseChildren(of element: Element) throws -> [ContentBlock] {
        var blocks: [ContentBlock] = []
        for child in element.children() {
            let tag = child.tagName().lowercased()
            switch tag {
            case "h1", "h2", "h3", "h4", "h5", "h6":
                let level = Int(tag.dropFirst()) ?? 1
                blocks.append(.heading(level: level, text: try child.text()))
            case "p":
                let spans = try parseSpans(child)
                if !spans.isEmpty { blocks.append(.paragraph(spans: spans)) }
            case "blockquote":
                blocks.append(.quote(text: try child.text()))
            case "img":
                if let src = try? child.attr("src"), !src.isEmpty {
                    let alt = (try? child.attr("alt")).flatMap { $0.isEmpty ? nil : $0 }
                    blocks.append(.image(url: src, caption: alt))
                }
            case "figure":
                if let img = try child.select("img").first() {
                    let src = try img.attr("src")
                    if !src.isEmpty {
                        let caption = (try? child.select("figcaption").first()?.text())
                            .flatMap { $0.isEmpty ? nil : $0 }
                        blocks.append(.image(url: src, caption: caption))
                    }
                }
            case "div", "article", "section", "main":
                blocks += try parseChildren(of: child)
            default:
                break
            }
        }
        return blocks
    }

    private static func parseSpans(_ element: Element) throws -> [TextSpan] {
        var spans: [TextSpan] = []
        for node in element.getChildNodes() {
            if let textNode = node as? TextNode {
                let text = textNode.text().trimmingCharacters(in: .whitespaces)
                if !text.isEmpty { spans.append(.plain(text)) }
            } else if let el = node as? Element {
                let tag = el.tagName().lowercased()
                let text = try el.text()
                switch tag {
                case "strong", "b": spans.append(.bold(text))
                case "em", "i":    spans.append(.italic(text))
                case "a":
                    let href = (try? el.attr("href")) ?? ""
                    spans.append(.link(text: text, url: href))
                default:
                    spans.append(.plain(text))
                }
            }
        }
        return spans
    }
}
```

**Step 4: Run tests — expect pass**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/HtmlToBlocksTests 2>&1 | grep -E "Test Suite|PASSED|FAILED"
```
Expected: all 5 tests PASSED.

**Step 5: Commit**

```bash
git add PavelRssReader-iOS/data/parser/HtmlToBlocks.swift PavelRssReader-iOSTests/HtmlToBlocksTests.swift
git commit -m "feat: add HtmlToBlocks parser with tests"
```

---

## Task 6: Network Services

**Files:**
- Create: `PavelRssReader-iOS/data/network/RssNetworkService.swift`
- Create: `PavelRssReader-iOS/data/network/ArticleContentFetcher.swift`

**Step 1: Create `RssNetworkService.swift`**

`data/network/RssNetworkService.swift`:
```swift
import Foundation

struct RssNetworkService {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func fetchFeedXml(from urlString: String) async throws -> String {
        guard let url = URL(string: urlString) else {
            throw AppError.networkError("Invalid URL: \(urlString)")
        }
        var request = URLRequest(url: url, timeoutInterval: 15)
        request.setValue("Mozilla/5.0 (compatible; RssReader/1.0)", forHTTPHeaderField: "User-Agent")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AppError.networkError("HTTP error fetching \(urlString)")
        }
        return String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: .isoLatin1)
            ?? ""
    }
}
```

**Step 2: Create `ArticleContentFetcher.swift`**

`data/network/ArticleContentFetcher.swift`:
```swift
import Foundation
import SwiftSoup

struct ArticleContentFetcher {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func fetchBlocks(from urlString: String) async throws -> [ContentBlock] {
        guard let url = URL(string: urlString) else {
            throw AppError.networkError("Invalid URL")
        }
        var request = URLRequest(url: url, timeoutInterval: 20)
        request.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36", forHTTPHeaderField: "User-Agent")
        let (data, _) = try await session.data(for: request)
        let html = String(data: data, encoding: .utf8) ?? ""
        return try extractBlocks(from: html)
    }

    private func extractBlocks(from html: String) throws -> [ContentBlock] {
        let doc = try SwiftSoup.parse(html)
        // Remove noise
        for selector in ["nav", "header", "footer", "aside", "[class*=ad]", "[class*=menu]",
                          "[class*=sidebar]", "[id*=sidebar]", "script", "style", "noscript"] {
            try doc.select(selector).remove()
        }
        // Priority selector list for article body
        let selectors = ["article", "[itemprop=articleBody]", "[class*=article-body]",
                         "[class*=article-content]", "[class*=post-body]",
                         "[class*=entry-content]", "main", ".content", "#content"]
        for selector in selectors {
            if let el = try doc.select(selector).first(), try !el.text().isEmpty {
                return try HtmlToBlocks.convert(try el.outerHtml())
            }
        }
        // Fallback: whole body
        return try HtmlToBlocks.convert(html)
    }
}
```

**Step 3: Build and commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/data/network
git commit -m "feat: add RssNetworkService and ArticleContentFetcher"
```

---

## Task 7: Repository Implementations

**Files:**
- Create: `PavelRssReader-iOS/data/repository/RssRepositoryImpl.swift`
- Create: `PavelRssReader-iOS/data/repository/SettingsRepositoryImpl.swift`

**Step 1: Create `SettingsRepositoryImpl.swift`**

`data/repository/SettingsRepositoryImpl.swift`:
```swift
import Foundation

final class SettingsRepositoryImpl: SettingsRepositoryProtocol {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var titleFontSize: Float {
        get { let v = defaults.float(forKey: "titleFontSize"); return v == 0 ? 14 : v }
    }
    var bodyFontSize: Float {
        get { let v = defaults.float(forKey: "bodyFontSize"); return v == 0 ? 17 : v }
    }
    var themePreference: ThemePreference {
        get {
            ThemePreference(rawValue: defaults.string(forKey: "themePreference") ?? "") ?? .system
        }
    }

    func setTitleFontSize(_ size: Float) { defaults.set(size, forKey: "titleFontSize") }
    func setBodyFontSize(_ size: Float)  { defaults.set(size, forKey: "bodyFontSize") }
    func setThemePreference(_ pref: ThemePreference) {
        defaults.set(pref.rawValue, forKey: "themePreference")
    }
}
```

**Step 2: Create `RssRepositoryImpl.swift`**

`data/repository/RssRepositoryImpl.swift`:
```swift
import Foundation
import SwiftData

final class RssRepositoryImpl: RssRepositoryProtocol {
    private let container: ModelContainer
    private let networkService: RssNetworkService
    private let parser: RssParser

    init(container: ModelContainer,
         networkService: RssNetworkService = RssNetworkService(),
         parser: RssParser = RssParser()) {
        self.container = container
        self.networkService = networkService
        self.parser = parser
    }

    @MainActor
    private var context: ModelContext { container.mainContext }

    // MARK: — Articles

    @MainActor
    func getAllArticles() async throws -> [Article] {
        let descriptor = FetchDescriptor<ArticleModel>(
            predicate: #Predicate { !$0.isRead },
            sortBy: [SortDescriptor(\.publishedAt, order: .reverse)]
        )
        return try context.fetch(descriptor).map { $0.toDomain() }
    }

    @MainActor
    func getFavouriteArticles() async throws -> [Article] {
        let descriptor = FetchDescriptor<ArticleModel>(
            predicate: #Predicate { $0.isFavorite },
            sortBy: [SortDescriptor(\.publishedAt, order: .reverse)]
        )
        return try context.fetch(descriptor).map { $0.toDomain() }
    }

    @MainActor
    func markAsRead(id: UUID) async throws {
        try findArticle(id: id)?.isRead = true
        try context.save()
    }

    @MainActor
    func markAsUnread(id: UUID) async throws {
        try findArticle(id: id)?.isRead = false
        try context.save()
    }

    @MainActor
    func setFavourite(id: UUID, isFavorite: Bool) async throws {
        try findArticle(id: id)?.isFavorite = isFavorite
        try context.save()
    }

    @MainActor
    func getNextUnreadArticle(after articleId: UUID, feedId: UUID?) async throws -> Article? {
        var predicate: Predicate<ArticleModel>
        if let feedId {
            predicate = #Predicate<ArticleModel> { !$0.isRead && $0.feedId == feedId }
        } else {
            predicate = #Predicate<ArticleModel> { !$0.isRead }
        }
        let descriptor = FetchDescriptor<ArticleModel>(
            predicate: predicate,
            sortBy: [SortDescriptor(\.publishedAt, order: .reverse)]
        )
        let all = try context.fetch(descriptor)
        guard let idx = all.firstIndex(where: { $0.id == articleId }),
              idx + 1 < all.count else { return nil }
        return all[idx + 1].toDomain()
    }

    // MARK: — Feeds

    @MainActor
    func getAllFeeds() async throws -> [Feed] {
        let descriptor = FetchDescriptor<FeedModel>(sortBy: [SortDescriptor(\.addedAt)])
        return try context.fetch(descriptor).map { $0.toDomain() }
    }

    @MainActor
    func getUnreadCountsPerFeed() async throws -> [FeedUnreadCount] {
        let descriptor = FetchDescriptor<FeedModel>()
        let feeds = try context.fetch(descriptor)
        return feeds.map { feed in
            FeedUnreadCount(feedId: feed.id, count: feed.articles.filter { !$0.isRead }.count)
        }
    }

    func addFeed(url: String) async throws {
        let xmlString = try await networkService.fetchFeedXml(from: url)
        let parsed = try parser.parse(xmlString: xmlString)
        let feedTitle = parsed.title.isEmpty ? url : parsed.title

        await MainActor.run {
            // Check for duplicate feed URL
            let existing = try? context.fetch(FetchDescriptor<FeedModel>(
                predicate: #Predicate { $0.url == url }
            ))
            guard existing?.isEmpty != false else { return }

            let feedModel = FeedModel(url: url, title: feedTitle)
            context.insert(feedModel)
            for item in parsed.items {
                let article = ArticleModel(
                    feedId: feedModel.id, guid: item.guid, title: item.title,
                    link: item.link, articleDescription: item.description,
                    publishedAt: item.publishedAt, sourceName: feedTitle,
                    imageUrl: item.imageUrl
                )
                article.feed = feedModel
                context.insert(article)
            }
            try? context.save()
        }
    }

    @MainActor
    func deleteFeed(id: UUID) async throws {
        guard let feed = try context.fetch(FetchDescriptor<FeedModel>(
            predicate: #Predicate { $0.id == id }
        )).first else { return }
        context.delete(feed)
        try context.save()
    }

    func refreshFeeds() async throws {
        let feeds = try await getAllFeeds()
        for feed in feeds {
            do {
                let xmlString = try await networkService.fetchFeedXml(from: feed.url)
                let parsed = try parser.parse(xmlString: xmlString)
                await MainActor.run {
                    // Prune expired non-favourite articles
                    let cutoff = Date().addingTimeInterval(-86_400)
                    let expiredDesc = FetchDescriptor<ArticleModel>(
                        predicate: #Predicate { !$0.isFavorite && !$0.isRead && $0.fetchedAt < cutoff }
                    )
                    if let expired = try? context.fetch(expiredDesc) {
                        expired.forEach { context.delete($0) }
                    }
                    // Insert new articles (skip duplicates by guid)
                    let existingGuids = (try? context.fetch(FetchDescriptor<ArticleModel>(
                        predicate: #Predicate { $0.feedId == feed.id }
                    )).map(\.guid)) ?? []
                    for item in parsed.items where !existingGuids.contains(item.guid) {
                        let article = ArticleModel(
                            feedId: feed.id, guid: item.guid, title: item.title,
                            link: item.link, articleDescription: item.description,
                            publishedAt: item.publishedAt, sourceName: feed.title,
                            imageUrl: item.imageUrl
                        )
                        context.insert(article)
                    }
                    try? context.save()
                }
            } catch {
                // Skip individual feed errors; continue refreshing others
            }
        }
    }

    // MARK: — Helpers

    @MainActor @discardableResult
    private func findArticle(id: UUID) throws -> ArticleModel? {
        try context.fetch(FetchDescriptor<ArticleModel>(
            predicate: #Predicate { $0.id == id }
        )).first
    }
}
```

**Step 3: Build and commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/data/repository
git commit -m "feat: add RssRepositoryImpl and SettingsRepositoryImpl"
```

---

## Task 8: App Entry, ModelContainer, Root Navigation

**Files:**
- Modify: `PavelRssReader-iOS/PavelRssReaderApp.swift`
- Create: `PavelRssReader-iOS/presentation/navigation/RootTabView.swift`
- Create: `PavelRssReader-iOS/presentation/navigation/AppEnvironment.swift`

**Step 1: Create `AppEnvironment.swift`**

`presentation/navigation/AppEnvironment.swift`:
```swift
import SwiftData

/// Holds app-wide shared dependencies; injected via @Environment.
@Observable
final class AppEnvironment {
    let rssRepository: RssRepositoryProtocol
    let settingsRepository: SettingsRepositoryProtocol

    init(container: ModelContainer) {
        rssRepository = RssRepositoryImpl(container: container)
        settingsRepository = SettingsRepositoryImpl()
    }
}
```

**Step 2: Create `RootTabView.swift`**

`presentation/navigation/RootTabView.swift`:
```swift
import SwiftUI

struct RootTabView: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        TabView {
            NavigationStack {
                ArticleListView(viewModel: ArticleListViewModel(repository: env.rssRepository))
            }
            .tabItem { Label("News", systemImage: "newspaper") }

            NavigationStack {
                FavouritesView(viewModel: FavouritesViewModel(repository: env.rssRepository))
            }
            .tabItem { Label("Favorites", systemImage: "star") }

            NavigationStack {
                FeedsView(viewModel: FeedsViewModel(repository: env.rssRepository))
            }
            .tabItem { Label("Feeds", systemImage: "antenna.radiowaves.left.and.right") }

            NavigationStack {
                SettingsView(viewModel: SettingsViewModel(settings: env.settingsRepository))
            }
            .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}
```

**Step 3: Rewrite `PavelRssReaderApp.swift`**

```swift
import SwiftUI
import SwiftData

@main
struct PavelRssReaderApp: App {
    private let container: ModelContainer
    private let appEnv: AppEnvironment

    init() {
        do {
            container = try ModelContainer(for: FeedModel.self, ArticleModel.self)
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
        appEnv = AppEnvironment(container: container)
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environment(appEnv)
        }
    }
}
```

> **Note:** At this point `ArticleListView`, `FavouritesView`, `FeedsView`, and `SettingsView` don't exist yet. Add temporary placeholder structs in stubs so the project builds. Create a file `presentation/navigation/ViewStubs.swift` with placeholder views, then delete it after implementing the real views.

`presentation/navigation/ViewStubs.swift` (temporary):
```swift
import SwiftUI
struct ArticleListView: View { let viewModel: ArticleListViewModel; var body: some View { Text("Articles") } }
struct FavouritesView: View { let viewModel: FavouritesViewModel; var body: some View { Text("Favourites") } }
struct FeedsView: View { let viewModel: FeedsViewModel; var body: some View { Text("Feeds") } }
struct SettingsView: View { let viewModel: SettingsViewModel; var body: some View { Text("Settings") } }
struct ArticleListViewModel { init(repository: RssRepositoryProtocol) {} }
struct FavouritesViewModel { init(repository: RssRepositoryProtocol) {} }
struct FeedsViewModel { init(repository: RssRepositoryProtocol) {} }
struct SettingsViewModel { init(settings: SettingsRepositoryProtocol) {} }
```

**Step 4: Build and commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/
git commit -m "feat: app entry point, ModelContainer, root tab navigation"
```

---

## Task 9: ArticleList — ViewModel (TDD) + View + ArticleCard

**Files:**
- Delete: `presentation/navigation/ViewStubs.swift` (the ArticleListViewModel/View stubs)
- Create: `PavelRssReader-iOS/presentation/articles/ArticleListViewModel.swift`
- Create: `PavelRssReader-iOS/presentation/articles/ArticleListView.swift`
- Create: `PavelRssReader-iOS/presentation/articles/ArticleCard.swift`
- Create: `PavelRssReader-iOSTests/ArticleListViewModelTests.swift`

**Step 1: Write failing ViewModel tests**

`PavelRssReader-iOSTests/ArticleListViewModelTests.swift`:
```swift
import XCTest
@testable import PavelRssReader_iOS

@MainActor
final class ArticleListViewModelTests: XCTestCase {

    func makeArticle(id: UUID = UUID(), feedId: UUID = UUID(), isRead: Bool = false) -> Article {
        Article(id: id, feedId: feedId, guid: "g", title: "T", link: "L",
                description: "", publishedAt: Date(), fetchedAt: Date(),
                isRead: isRead, isFavorite: false, imageUrl: nil, sourceName: "Src")
    }

    func test_load_populatesArticles() async throws {
        let repo = MockRssRepository()
        let article = makeArticle()
        repo.stubbedArticles = [article]
        let vm = ArticleListViewModel(repository: repo)
        await vm.load()
        XCTAssertEqual(vm.articles.count, 1)
    }

    func test_displayedArticles_excludesHidden() async throws {
        let repo = MockRssRepository()
        let id = UUID()
        repo.stubbedArticles = [makeArticle(id: id)]
        let vm = ArticleListViewModel(repository: repo)
        await vm.load()
        await vm.dismissArticle(id)
        XCTAssertEqual(vm.displayedArticles.count, 0)
    }

    func test_undoDismiss_restoresArticle() async throws {
        let repo = MockRssRepository()
        let id = UUID()
        repo.stubbedArticles = [makeArticle(id: id)]
        let vm = ArticleListViewModel(repository: repo)
        await vm.load()
        await vm.dismissArticle(id)
        vm.undoDismiss(id)
        XCTAssertEqual(vm.displayedArticles.count, 1)
    }

    func test_selectFeed_filtersArticles() async throws {
        let repo = MockRssRepository()
        let feedA = UUID(); let feedB = UUID()
        repo.stubbedArticles = [makeArticle(feedId: feedA), makeArticle(feedId: feedB)]
        let vm = ArticleListViewModel(repository: repo)
        await vm.load()
        vm.selectFeed(feedA)
        XCTAssertEqual(vm.displayedArticles.count, 1)
        XCTAssertEqual(vm.displayedArticles.first?.feedId, feedA)
    }
}

// MARK: — Mock

final class MockRssRepository: RssRepositoryProtocol {
    var stubbedArticles: [Article] = []
    var stubbedFeeds: [Feed] = []
    var markedReadIds: [UUID] = []
    var markedUnreadIds: [UUID] = []
    var toggledFavouriteIds: [(UUID, Bool)] = []

    func getAllArticles() async throws -> [Article] { stubbedArticles }
    func getFavouriteArticles() async throws -> [Article] { stubbedArticles.filter { $0.isFavorite } }
    func getAllFeeds() async throws -> [Feed] { stubbedFeeds }
    func getUnreadCountsPerFeed() async throws -> [FeedUnreadCount] { [] }
    func addFeed(url: String) async throws {}
    func deleteFeed(id: UUID) async throws {}
    func refreshFeeds() async throws {}
    func setFavourite(id: UUID, isFavorite: Bool) async throws { toggledFavouriteIds.append((id, isFavorite)) }
    func markAsRead(id: UUID) async throws { markedReadIds.append(id) }
    func markAsUnread(id: UUID) async throws { markedUnreadIds.append(id) }
    func getNextUnreadArticle(after: UUID, feedId: UUID?) async throws -> Article? { nil }
}
```

**Step 2: Run tests — expect failure**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/ArticleListViewModelTests 2>&1 | grep -E "error:|FAILED|PASSED" | head -5
```

**Step 3: Create `ArticleListViewModel.swift`**

`presentation/articles/ArticleListViewModel.swift`:
```swift
import Foundation

@Observable
final class ArticleListViewModel {
    var articles: [Article] = []
    var feeds: [Feed] = []
    var selectedFeedId: UUID? = nil
    var hiddenArticleIds: Set<UUID> = []
    var isRefreshing = false
    var error: String? = nil

    var displayedArticles: [Article] {
        articles
            .filter { !$0.isRead }
            .filter { !hiddenArticleIds.contains($0.id) }
            .filter { selectedFeedId == nil || $0.feedId == selectedFeedId }
    }

    private let getArticles: GetArticlesUseCase
    private let getFeeds: GetFeedsUseCase
    private let refreshUseCase: RefreshFeedsUseCase
    private let markRead: MarkAsReadUseCase
    private let markUnread: UnmarkAsReadUseCase
    private let toggleFav: ToggleFavouriteUseCase

    init(repository: RssRepositoryProtocol) {
        getArticles  = GetArticlesUseCase(repository: repository)
        getFeeds     = GetFeedsUseCase(repository: repository)
        refreshUseCase = RefreshFeedsUseCase(repository: repository)
        markRead     = MarkAsReadUseCase(repository: repository)
        markUnread   = UnmarkAsReadUseCase(repository: repository)
        toggleFav    = ToggleFavouriteUseCase(repository: repository)
    }

    @MainActor func load() async {
        do {
            async let a = getArticles.execute()
            async let f = getFeeds.execute()
            articles = try await a
            feeds    = try await f
        } catch { self.error = error.localizedDescription }
    }

    @MainActor func refresh() async {
        isRefreshing = true
        do { try await refreshUseCase.execute(); await load() }
        catch { self.error = error.localizedDescription }
        isRefreshing = false
    }

    func selectFeed(_ id: UUID?) { selectedFeedId = id }

    @MainActor func dismissArticle(_ id: UUID) async {
        hiddenArticleIds.insert(id)
        do { try await markRead.execute(id: id) }
        catch { self.error = error.localizedDescription }
    }

    func undoDismiss(_ id: UUID) {
        hiddenArticleIds.remove(id)
        Task { try? await markUnread.execute(id: id) }
    }

    func confirmDismiss(_ id: UUID) { hiddenArticleIds.remove(id) }

    @MainActor func toggleFavourite(_ article: Article) async {
        do { try await toggleFav.execute(id: article.id, isFavorite: !article.isFavorite); await load() }
        catch { self.error = error.localizedDescription }
    }

    func clearError() { error = nil }
}
```

**Step 4: Run tests — expect pass**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/ArticleListViewModelTests 2>&1 | grep -E "Test Suite|PASSED|FAILED"
```

**Step 5: Create `ArticleCard.swift`**

`presentation/articles/ArticleCard.swift`:
```swift
import SwiftUI

struct ArticleCard: View {
    let article: Article
    let onStarTap: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(article.sourceName) • \(relativeTime(article.publishedAt))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(article.title)
                    .font(.headline)
                    .lineLimit(3)
            }
            Spacer()
            Button(action: onStarTap) {
                Image(systemName: article.isFavorite ? "star.fill" : "star")
                    .foregroundStyle(article.isFavorite ? .yellow : .secondary)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 8)
    }

    private func relativeTime(_ date: Date) -> String {
        let diff = Date().timeIntervalSince(date)
        if diff < 60          { return "Just now" }
        if diff < 3600        { return "\(Int(diff / 60))m ago" }
        if diff < 86400       { return "\(Int(diff / 3600))h ago" }
        let fmt = DateFormatter(); fmt.dateFormat = "MMM d"
        return fmt.string(from: date)
    }
}
```

**Step 6: Create `ArticleListView.swift`**

`presentation/articles/ArticleListView.swift`:
```swift
import SwiftUI

struct ArticleListView: View {
    @State var viewModel: ArticleListViewModel
    @State private var undoArticleId: UUID? = nil
    @State private var showUndo = false

    var body: some View {
        List {
            // Source filter picker
            if !viewModel.feeds.isEmpty {
                Picker("Filter", selection: Binding(
                    get: { viewModel.selectedFeedId },
                    set: { viewModel.selectFeed($0) }
                )) {
                    Text("All").tag(UUID?.none)
                    ForEach(viewModel.feeds) { feed in
                        Text(feed.title).tag(UUID?.some(feed.id))
                    }
                }
                .pickerStyle(.menu)
            }

            ForEach(viewModel.displayedArticles) { article in
                NavigationLink(value: article) {
                    ArticleCard(article: article) {
                        Task { await viewModel.toggleFavourite(article) }
                    }
                }
                .swipeActions(edge: .leading) {
                    Button("Read") {
                        let generator = UIImpactFeedbackGenerator(style: .medium)
                        generator.impactOccurred()
                        undoArticleId = article.id
                        showUndo = true
                        Task { await viewModel.dismissArticle(article.id) }
                    }
                    .tint(.blue)
                }
            }
        }
        .navigationTitle("News")
        .refreshable { await viewModel.refresh() }
        .navigationDestination(for: Article.self) { article in
            ArticleReaderView(viewModel: ArticleReaderViewModel(
                articleId: article.id,
                feedId: article.feedId,
                repository: viewModel.rssRepository,
                settings: viewModel.settingsRepository
            ))
        }
        .overlay(alignment: .bottom) {
            if showUndo {
                UndoToast(message: "Marked as read") {
                    if let id = undoArticleId { viewModel.undoDismiss(id) }
                    showUndo = false
                }
                .padding(.bottom, 80)
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        if let id = undoArticleId { viewModel.confirmDismiss(id) }
                        showUndo = false
                    }
                }
            }
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.error != nil },
            set: { if !$0 { viewModel.clearError() } }
        )) {
            Button("OK") { viewModel.clearError() }
        } message: {
            Text(viewModel.error ?? "")
        }
        .task { await viewModel.load() }
    }
}
```

> **Note:** `ArticleListViewModel` needs to expose `rssRepository` and `settingsRepository` for `NavigationDestination`. Add these two stored properties to `ArticleListViewModel`:
> ```swift
> let rssRepository: RssRepositoryProtocol
> let settingsRepository: SettingsRepositoryProtocol = SettingsRepositoryImpl()
> ```
> and set `self.rssRepository = repository` in the `init`.

**Step 7: Create `UndoToast.swift`**

`presentation/articles/UndoToast.swift`:
```swift
import SwiftUI

struct UndoToast: View {
    let message: String
    let onUndo: () -> Void

    var body: some View {
        HStack {
            Text(message)
            Spacer()
            Button("Undo", action: onUndo)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}
```

**Step 8: Remove ArticleListViewModel/View stubs, build**

Delete the corresponding stub lines from `ViewStubs.swift`, then:
```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
```

**Step 9: Commit**

```bash
git add PavelRssReader-iOS/presentation/articles PavelRssReader-iOSTests/ArticleListViewModelTests.swift
git commit -m "feat: ArticleListViewModel (tested) + ArticleListView + ArticleCard"
```

---

## Task 10: Feeds — ViewModel (TDD) + View

**Files:**
- Create: `PavelRssReader-iOS/presentation/feeds/FeedsViewModel.swift`
- Create: `PavelRssReader-iOS/presentation/feeds/FeedsView.swift`
- Create: `PavelRssReader-iOSTests/FeedsViewModelTests.swift`

**Step 1: Write failing tests**

`PavelRssReader-iOSTests/FeedsViewModelTests.swift`:
```swift
import XCTest
@testable import PavelRssReader_iOS

@MainActor
final class FeedsViewModelTests: XCTestCase {
    func makeFeed(id: UUID = UUID()) -> Feed {
        Feed(id: id, url: "https://example.com", title: "Feed", addedAt: Date())
    }

    func test_load_populatesFeeds() async {
        let repo = MockRssRepository()
        repo.stubbedFeeds = [makeFeed()]
        let vm = FeedsViewModel(repository: repo)
        await vm.load()
        XCTAssertEqual(vm.feeds.count, 1)
    }

    func test_deleteFeed_callsRepository() async throws {
        let repo = MockRssRepository()
        let id = UUID()
        repo.stubbedFeeds = [makeFeed(id: id)]
        let vm = FeedsViewModel(repository: repo)
        await vm.load()
        await vm.deleteFeed(id: id)
        XCTAssertEqual(repo.deletedFeedIds, [id])
    }
}
```

Also add to `MockRssRepository`:
```swift
var deletedFeedIds: [UUID] = []
// update deleteFeed:
func deleteFeed(id: UUID) async throws { deletedFeedIds.append(id) }
```

**Step 2: Create `FeedsViewModel.swift`**

`presentation/feeds/FeedsViewModel.swift`:
```swift
import Foundation

@Observable
final class FeedsViewModel {
    var feeds: [Feed] = []
    var unreadCounts: [UUID: Int] = [:]
    var isAddingFeed = false
    var addFeedError: String? = nil
    var feedAdded = false

    private let getFeeds: GetFeedsUseCase
    private let getCounts: GetUnreadCountsPerFeedUseCase
    private let addFeedUseCase: AddFeedUseCase
    private let deleteFeedUseCase: DeleteFeedUseCase

    init(repository: RssRepositoryProtocol) {
        getFeeds        = GetFeedsUseCase(repository: repository)
        getCounts       = GetUnreadCountsPerFeedUseCase(repository: repository)
        addFeedUseCase  = AddFeedUseCase(repository: repository)
        deleteFeedUseCase = DeleteFeedUseCase(repository: repository)
    }

    @MainActor func load() async {
        do {
            async let f = getFeeds.execute()
            async let c = getCounts.execute()
            feeds = try await f
            unreadCounts = Dictionary(uniqueKeysWithValues: try await c.map { ($0.feedId, $0.count) })
        } catch {}
    }

    @MainActor func addFeed(url: String) async {
        isAddingFeed = true; addFeedError = nil
        do {
            try await addFeedUseCase.execute(url: url)
            feedAdded = true
            await load()
        } catch { addFeedError = error.localizedDescription }
        isAddingFeed = false
    }

    @MainActor func deleteFeed(id: UUID) async {
        do { try await deleteFeedUseCase.execute(id: id); await load() }
        catch {}
    }

    func clearAddFeedError() { addFeedError = nil }
}
```

**Step 3: Run tests**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/FeedsViewModelTests 2>&1 | grep -E "PASSED|FAILED"
```

**Step 4: Create `FeedsView.swift`**

`presentation/feeds/FeedsView.swift`:
```swift
import SwiftUI

struct FeedsView: View {
    @State var viewModel: FeedsViewModel
    @State private var showAddSheet = false
    @State private var newFeedUrl = ""
    @State private var feedToDelete: UUID? = nil

    var body: some View {
        List {
            ForEach(viewModel.feeds) { feed in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(feed.title).font(.headline)
                        Text(feed.url).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                    }
                    Spacer()
                    if let count = viewModel.unreadCounts[feed.id], count > 0 {
                        Text("\(count)")
                            .font(.caption.bold())
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.accentColor)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) { feedToDelete = feed.id } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .navigationTitle("Feeds")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAddSheet = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            AddFeedSheet(viewModel: viewModel, isPresented: $showAddSheet)
        }
        .alert("Delete Feed?", isPresented: Binding(
            get: { feedToDelete != nil },
            set: { if !$0 { feedToDelete = nil } }
        )) {
            Button("Delete", role: .destructive) {
                if let id = feedToDelete { Task { await viewModel.deleteFeed(id: id) } }
                feedToDelete = nil
            }
            Button("Cancel", role: .cancel) { feedToDelete = nil }
        } message: {
            Text("This will delete all articles from this feed.")
        }
        .task { await viewModel.load() }
    }
}

private struct AddFeedSheet: View {
    @State var viewModel: FeedsViewModel
    @Binding var isPresented: Bool
    @State private var url = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Feed URL (https://...)", text: $url)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                if let err = viewModel.addFeedError {
                    Text(err).foregroundStyle(.red).font(.caption)
                }
            }
            .navigationTitle("Add Feed")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isPresented = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if viewModel.isAddingFeed {
                        ProgressView()
                    } else {
                        Button("Add") {
                            Task {
                                await viewModel.addFeed(url: url)
                                if viewModel.addFeedError == nil { isPresented = false }
                            }
                        }
                        .disabled(url.isEmpty)
                    }
                }
            }
        }
    }
}
```

**Step 5: Remove FeedsViewModel/View stubs, build, commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/presentation/feeds PavelRssReader-iOSTests/FeedsViewModelTests.swift
git commit -m "feat: FeedsViewModel (tested) + FeedsView with add/delete"
```

---

## Task 11: Favourites — ViewModel + View

**Files:**
- Create: `PavelRssReader-iOS/presentation/favourites/FavouritesViewModel.swift`
- Create: `PavelRssReader-iOS/presentation/favourites/FavouritesView.swift`

**Step 1: Create `FavouritesViewModel.swift`**

`presentation/favourites/FavouritesViewModel.swift`:
```swift
import Foundation

@Observable
final class FavouritesViewModel {
    var favourites: [Article] = []

    private let getFavourites: GetFavouritesUseCase
    private let toggleFav: ToggleFavouriteUseCase

    init(repository: RssRepositoryProtocol) {
        getFavourites = GetFavouritesUseCase(repository: repository)
        toggleFav     = ToggleFavouriteUseCase(repository: repository)
    }

    @MainActor func load() async {
        favourites = (try? await getFavourites.execute()) ?? []
    }

    @MainActor func removeFavourite(id: UUID) async {
        try? await toggleFav.execute(id: id, isFavorite: false)
        await load()
    }
}
```

**Step 2: Create `FavouritesView.swift`**

`presentation/favourites/FavouritesView.swift`:
```swift
import SwiftUI

struct FavouritesView: View {
    @State var viewModel: FavouritesViewModel

    var body: some View {
        Group {
            if viewModel.favourites.isEmpty {
                ContentUnavailableView("No Favourites", systemImage: "star.slash",
                    description: Text("Star articles to save them here."))
            } else {
                List {
                    ForEach(viewModel.favourites) { article in
                        NavigationLink(value: article) {
                            ArticleCard(article: article) {
                                Task { await viewModel.removeFavourite(id: article.id) }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Favorites")
        .task { await viewModel.load() }
    }
}
```

**Step 3: Remove FavouritesViewModel/View stubs, build, commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/presentation/favourites
git commit -m "feat: FavouritesViewModel + FavouritesView"
```

---

## Task 12: Article Reader — ViewModel (TDD) + View

**Files:**
- Create: `PavelRssReader-iOS/presentation/reader/ArticleReaderViewModel.swift`
- Create: `PavelRssReader-iOS/presentation/reader/ArticleReaderView.swift`
- Create: `PavelRssReader-iOSTests/ArticleReaderViewModelTests.swift`

**Step 1: Write failing tests**

`PavelRssReader-iOSTests/ArticleReaderViewModelTests.swift`:
```swift
import XCTest
@testable import PavelRssReader_iOS

@MainActor
final class ArticleReaderViewModelTests: XCTestCase {

    func test_loadArticle_setsIsLoadingFalseAfterLoad() async {
        let repo = MockRssRepository()
        let id = UUID()
        repo.stubbedArticles = [Article(id: id, feedId: UUID(), guid: "g", title: "T",
            link: "https://example.com", description: "", publishedAt: Date(),
            fetchedAt: Date(), isRead: false, isFavorite: false, imageUrl: nil, sourceName: "S")]
        let vm = ArticleReaderViewModel(articleId: id, feedId: UUID(),
                                        repository: repo, settings: MockSettingsRepository())
        await vm.load()
        XCTAssertFalse(vm.isLoading)
    }

    func test_loadArticle_marksArticleAsRead() async {
        let repo = MockRssRepository()
        let id = UUID()
        repo.stubbedArticles = [Article(id: id, feedId: UUID(), guid: "g", title: "T",
            link: "https://example.com", description: "", publishedAt: Date(),
            fetchedAt: Date(), isRead: false, isFavorite: false, imageUrl: nil, sourceName: "S")]
        let vm = ArticleReaderViewModel(articleId: id, feedId: UUID(),
                                        repository: repo, settings: MockSettingsRepository())
        await vm.load()
        XCTAssertTrue(repo.markedReadIds.contains(id))
    }
}

final class MockSettingsRepository: SettingsRepositoryProtocol {
    var titleFontSize: Float = 14
    var bodyFontSize: Float = 17
    var themePreference: ThemePreference = .system
    func setTitleFontSize(_ size: Float) { titleFontSize = size }
    func setBodyFontSize(_ size: Float) { bodyFontSize = size }
    func setThemePreference(_ pref: ThemePreference) { themePreference = pref }
}
```

**Step 2: Create `ArticleReaderViewModel.swift`**

`presentation/reader/ArticleReaderViewModel.swift`:
```swift
import Foundation

@Observable
final class ArticleReaderViewModel {
    var contentBlocks: [ContentBlock] = []
    var article: Article? = nil
    var isLoading = false
    var error: String? = nil
    var titleFontSize: Float
    var bodyFontSize: Float

    private let articleId: UUID
    private let feedId: UUID
    private let repository: RssRepositoryProtocol
    private let fetcher = ArticleContentFetcher()
    private let markRead: MarkAsReadUseCase
    private let toggleFav: ToggleFavouriteUseCase

    init(articleId: UUID, feedId: UUID,
         repository: RssRepositoryProtocol,
         settings: SettingsRepositoryProtocol) {
        self.articleId = articleId
        self.feedId = feedId
        self.repository = repository
        self.titleFontSize = settings.titleFontSize
        self.bodyFontSize  = settings.bodyFontSize
        self.markRead   = MarkAsReadUseCase(repository: repository)
        self.toggleFav  = ToggleFavouriteUseCase(repository: repository)
    }

    @MainActor func load() async {
        isLoading = true
        do {
            try await markRead.execute(id: articleId)
            let all = try await repository.getAllArticles()
            article = all.first { $0.id == articleId }
            if let link = article?.link {
                contentBlocks = try await fetcher.fetchBlocks(from: link)
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    @MainActor func toggleFavourite() async {
        guard let a = article else { return }
        try? await toggleFav.execute(id: a.id, isFavorite: !a.isFavorite)
    }

    @MainActor func goToNextArticle() async -> Article? {
        try? await repository.getNextUnreadArticle(after: articleId, feedId: feedId)
    }
}
```

**Step 3: Run tests**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:PavelRssReader-iOSTests/ArticleReaderViewModelTests 2>&1 | grep -E "PASSED|FAILED"
```

**Step 4: Create `ArticleReaderView.swift`**

`presentation/reader/ArticleReaderView.swift`:
```swift
import SwiftUI

struct ArticleReaderView: View {
    @State var viewModel: ArticleReaderViewModel
    @State private var nextArticle: Article? = nil
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if let article = viewModel.article {
                    Text(article.title)
                        .font(.system(size: CGFloat(viewModel.titleFontSize), weight: .bold))
                    Text("\(article.sourceName) • \(article.publishedAt.formatted(.relative(presentation: .named)))")
                        .font(.caption).foregroundStyle(.secondary)
                    Divider()
                }

                if viewModel.isLoading {
                    ProgressView().frame(maxWidth: .infinity)
                } else {
                    ForEach(Array(viewModel.contentBlocks.enumerated()), id: \.offset) { _, block in
                        blockView(block)
                    }
                }

                Button("Next Article") {
                    Task {
                        if let next = await viewModel.goToNextArticle() { nextArticle = next }
                        else { dismiss() }
                    }
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
                .padding(.top, 24)
            }
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { Task { await viewModel.toggleFavourite() } } label: {
                    Image(systemName: viewModel.article?.isFavorite == true ? "star.fill" : "star")
                }
            }
        }
        .navigationDestination(item: $nextArticle) { article in
            ArticleReaderView(viewModel: ArticleReaderViewModel(
                articleId: article.id, feedId: article.feedId,
                repository: viewModel.rssRepository,
                settings: viewModel.settingsRepository
            ))
        }
        .task { await viewModel.load() }
    }

    @ViewBuilder
    private func blockView(_ block: ContentBlock) -> some View {
        switch block {
        case .heading(let level, let text):
            Text(text)
                .font(.system(size: CGFloat(level <= 2 ? 20 : 17), weight: level <= 2 ? .bold : .semibold))
        case .paragraph(let spans):
            Text(attributedString(from: spans))
                .font(.system(size: CGFloat(viewModel.bodyFontSize)))
        case .image(let url, let caption):
            VStack(alignment: .leading, spacing: 4) {
                AsyncImage(url: URL(string: url)) { img in
                    img.resizable().scaledToFit().cornerRadius(8)
                } placeholder: {
                    Rectangle().fill(.quaternary).frame(height: 200).cornerRadius(8)
                }
                if let cap = caption { Text(cap).font(.caption).foregroundStyle(.secondary) }
            }
        case .quote(let text):
            HStack(spacing: 12) {
                Rectangle().fill(Color.accentColor).frame(width: 3)
                Text(text).italic().foregroundStyle(.secondary)
                    .font(.system(size: CGFloat(viewModel.bodyFontSize)))
            }
        }
    }

    private func attributedString(from spans: [TextSpan]) -> AttributedString {
        spans.reduce(into: AttributedString()) { result, span in
            switch span {
            case .plain(let t):
                result += AttributedString(t)
            case .bold(let t):
                var s = AttributedString(t)
                s.font = .body.bold()
                result += s
            case .italic(let t):
                var s = AttributedString(t)
                s.font = .body.italic()
                result += s
            case .link(let t, let url):
                var s = AttributedString(t)
                s.link = URL(string: url)
                result += s
            }
        }
    }
}
```

> **Note:** Add `var rssRepository: RssRepositoryProtocol` and `var settingsRepository: SettingsRepositoryProtocol` as stored properties on `ArticleReaderViewModel` so `ArticleReaderView` can pass them to the next article's ViewModel.

**Step 5: Build, commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/presentation/reader PavelRssReader-iOSTests/ArticleReaderViewModelTests.swift
git commit -m "feat: ArticleReaderViewModel (tested) + ArticleReaderView"
```

---

## Task 13: Settings — ViewModel + SettingsView + FontSizeView

**Files:**
- Create: `PavelRssReader-iOS/presentation/settings/SettingsViewModel.swift`
- Create: `PavelRssReader-iOS/presentation/settings/SettingsView.swift`
- Create: `PavelRssReader-iOS/presentation/settings/FontSizeView.swift`

**Step 1: Create `SettingsViewModel.swift`**

`presentation/settings/SettingsViewModel.swift`:
```swift
import Foundation

@Observable
final class SettingsViewModel {
    var titleFontSize: Float
    var bodyFontSize: Float
    var themePreference: ThemePreference

    private let settings: SettingsRepositoryProtocol

    init(settings: SettingsRepositoryProtocol) {
        self.settings = settings
        titleFontSize   = settings.titleFontSize
        bodyFontSize    = settings.bodyFontSize
        themePreference = settings.themePreference
    }

    func setTitleFontSize(_ v: Float) { titleFontSize = v; settings.setTitleFontSize(v) }
    func setBodyFontSize(_ v: Float)  { bodyFontSize = v;  settings.setBodyFontSize(v) }
    func setThemePreference(_ v: ThemePreference) { themePreference = v; settings.setThemePreference(v) }
}
```

**Step 2: Create `SettingsView.swift`**

`presentation/settings/SettingsView.swift`:
```swift
import SwiftUI

struct SettingsView: View {
    @State var viewModel: SettingsViewModel
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        Form {
            Section("Appearance") {
                Picker("Theme", selection: Binding(
                    get: { viewModel.themePreference },
                    set: { viewModel.setThemePreference($0) }
                )) {
                    ForEach(ThemePreference.allCases, id: \.self) { pref in
                        Text(pref.rawValue.capitalized).tag(pref)
                    }
                }
                .pickerStyle(.segmented)
            }

            Section("General") {
                NavigationLink("Text Size") {
                    FontSizeView(viewModel: viewModel)
                }
            }

            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
    }
}
```

**Step 3: Create `FontSizeView.swift`**

`presentation/settings/FontSizeView.swift`:
```swift
import SwiftUI

struct FontSizeView: View {
    @State var viewModel: SettingsViewModel

    var body: some View {
        Form {
            Section("Article Title") {
                Slider(value: Binding(
                    get: { Double(viewModel.titleFontSize) },
                    set: { viewModel.setTitleFontSize(Float($0)) }
                ), in: 10...22, step: 1)
                Text("Preview Title")
                    .font(.system(size: CGFloat(viewModel.titleFontSize), weight: .bold))
                    .frame(maxWidth: .infinity, alignment: .center)
            }

            Section("Article Body") {
                Slider(value: Binding(
                    get: { Double(viewModel.bodyFontSize) },
                    set: { viewModel.setBodyFontSize(Float($0)) }
                ), in: 12...28, step: 1)
                Text("The quick brown fox jumps over the lazy dog.")
                    .font(.system(size: CGFloat(viewModel.bodyFontSize)))
                    .frame(maxWidth: .infinity, alignment: .center)
            }
        }
        .navigationTitle("Text Size")
    }
}
```

**Step 4: Remove SettingsViewModel/View stubs, build, commit**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
git add PavelRssReader-iOS/presentation/settings
git commit -m "feat: SettingsViewModel + SettingsView + FontSizeView"
```

---

## Task 14: Theme, Colors, Final Wiring

**Files:**
- Create: `PavelRssReader-iOS/ui/theme/Colors.swift`
- Modify: `PavelRssReader-iOS/PavelRssReaderApp.swift` — apply theme
- Delete: `PavelRssReader-iOS/presentation/navigation/ViewStubs.swift` (should be empty by now)

**Step 1: Create `Colors.swift`**

`ui/theme/Colors.swift`:
```swift
import SwiftUI

extension Color {
    // Mirrors Android blue M3 palette (primary #0061A4)
    static let rssPrimary = Color(red: 0/255, green: 97/255, blue: 164/255)
}

extension View {
    func rssAccent() -> some View {
        self.tint(.rssPrimary)
    }
}
```

**Step 2: Apply theme preference to root view**

In `PavelRssReaderApp.swift`, update the `body`:
```swift
var body: some Scene {
    WindowGroup {
        RootTabView()
            .environment(appEnv)
            .preferredColorScheme(colorScheme(for: appEnv.settingsRepository.themePreference))
            .tint(.rssPrimary)
    }
}

private func colorScheme(for pref: ThemePreference) -> ColorScheme? {
    switch pref {
    case .system: return nil
    case .light:  return .light
    case .dark:   return .dark
    }
}
```

**Step 3: Delete ViewStubs.swift if it still exists**

```bash
rm -f ~/Documents/Ios_Dev/PavelRssReader-iOS/PavelRssReader-iOS/presentation/navigation/ViewStubs.swift
```

**Step 4: Run full test suite**

```bash
xcodebuild test -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' 2>&1 | grep -E "Test Suite|PASSED|FAILED|error:"
```
Expected: all tests PASSED, no errors.

**Step 5: Final build verification**

```bash
xcodebuild -scheme PavelRssReader-iOS -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -3
```
Expected: `** BUILD SUCCEEDED **`

**Step 6: Final commit**

```bash
git add -A
git commit -m "feat: complete iOS RSS Reader — theme, colors, full wiring"
```

---

## Summary

| Task | Deliverable |
|---|---|
| 1 | Xcode project via xcodegen, SwiftSoup SPM dependency |
| 2 | Domain layer: 6 models, 2 repository protocols, 10 use cases |
| 3 | SwiftData models (FeedModel, ArticleModel) + domain mappers |
| 4 | RssParser: RSS 2.0 + Atom XMLParser with 7 tests |
| 5 | HtmlToBlocks: HTML → ContentBlock converter with 5 tests |
| 6 | RssNetworkService (URLSession) + ArticleContentFetcher (SwiftSoup) |
| 7 | RssRepositoryImpl (SwiftData) + SettingsRepositoryImpl (UserDefaults) |
| 8 | App entry, ModelContainer, AppEnvironment DI, RootTabView |
| 9 | ArticleListViewModel (4 tests) + ArticleListView + ArticleCard + UndoToast |
| 10 | FeedsViewModel (2 tests) + FeedsView with add/delete + unread badges |
| 11 | FavouritesViewModel + FavouritesView |
| 12 | ArticleReaderViewModel (2 tests) + ArticleReaderView (native HTML renderer) |
| 13 | SettingsViewModel + SettingsView + FontSizeView |
| 14 | Colors (blue M3 palette), theme wiring, final cleanup |
