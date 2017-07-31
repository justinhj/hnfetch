# hnfetch

## Using Scala to interact with the Hacker News API

This is a simple program written to supplement a blog post on using Scala to access Hacker News users and stories.

[Hacker News API Part 1](http://justinhj.github.io/2017/07/26/hacker-news-api-1.html)

By running the main program you can view a text version of the Hacker News front page in your terminal (or IDE output).

Usage `sbt run 0` shows the first 10 items

`sbt run 1` shows the next 10 items and so on

## Example output 

```
1. Deep Learning for Coders – Launching Deep Learning Part 2 (www.fast.ai)
  331 points by jph00 at 8 hours ago 50 comments

2. PSA: The CopyFish OCR Chrome extension has been hijacked. Disable immediately (a9t9.com)
  156 points by timr at 6 hours ago 70 comments

3. Lindy effect (en.wikipedia.org)
  94 points by bushido at 5 hours ago 19 comments

4. We Evolved to Run But We're Doing It Wrong (news.nationalgeographic.com)
  169 points by thomyorkie at 8 hours ago 106 comments

5. Why I left Medium and moved back to my own domain (arslan.io)
  330 points by ingve at 15 hours ago 165 comments

6. Cryptocurrency miners are renting Boeing 747s to ship graphics cards (www.pcgamer.com)
  107 points by z3t1 at 6 hours ago 62 comments

7. LaTeX Math in MS Office (blogs.msdn.microsoft.com)
  75 points by ygra at 7 hours ago 17 comments

8. Jim Mellon and high-profile partners roll the dice on an anti-aging upstart (endpts.com)
  91 points by discombobulate at 8 hours ago 45 comments

9. Tools for Remote Software Development and Pair Programming (zapier.com)
Disconnected from the target VM, address: '127.0.0.1:59809', transport: 'socket'
  85 points by davidjnelson at 9 hours ago 19 comments

10. Implementing Algebraic Effects in C (www.microsoft.com)
  86 points by necrodome at 8 hours ago 5 comments

```


