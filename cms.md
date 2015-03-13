
## Notes on the cms changes in relation to drafts and live


see: [wiki](https://thesib.atlassian.net/wiki/display/SIB/CMS+Integration+with+Visual+Authoring)
and: [wiki drafts](https://thesib.atlassian.net/wiki/display/SIB/Draft+Items+for+Item+Editor)


A search result for an item can list the following: 

Status: Live/Draft

if Live: no of sessions
if Draft: no of revisions

So what exactly does live/draft mean?

Live: There is no drafts for this user.
Draft: There is a draft for this user.

Note: published - boolean is no longer relevant.

So if Gwen logs in and looks at Item 1:0 and she has a draft - she'll see 'Draft'
If Ed logs on and looks at Item 1:0 and has no draft - he'll see Live

Note: Due to the complexity and unpleasantness of the Search logic (and it's due to change), we'll prob need a separate call to get this info.

## Action: Make Draft

for item 1:0, make a draft for user Ed
for item 1:0, make a draft for user Gwen


## Missing Action: remove a draft

It may be that a user wants to discard their draft - but there is no option to do that.

## publish/unpublishet vs golive/draft

It may be the case that you want to create a draft, commit it but not make it available to students straight away.
eg you may want others to look at it and approve it.



The button status [Draft/Live] == published: false/true

If ed creates a draft of item 1:0 , when gwen logs in - she can't 'make a draft of 1:0' because the system will know that ed has a draft targeting 1:0 active. this applies for published/unpublished.

given item 1:0, if the status says [Draft], when you click edit - you create a 'user draft' with a src of 1:0, when you commit you don't save a new version you overwrite the existing item so you go back to 1:0
