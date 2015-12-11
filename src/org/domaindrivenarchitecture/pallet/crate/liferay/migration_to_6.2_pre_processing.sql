/* mainly not used */
delete from PollsVote;

/* removed group */
delete from DLFileEntry where groupid = '11259';
delete from DLFileVersion where groupid = '11259';
/* removed user */
update DLFileEntry set userId = 10485 where userId = 20538;
update DLFolder set userId = 10485 where userId = 20538;

/* MessageBoard: causing migration problems
 
Select a.threadFlagId, a.userId, count(*) 
from MBThreadFlag a
        inner join MBThreadFlag b on (a.userId = b.userId and a.threadFlagId = b.threadFlagId)
group by a.threadFlagId, a.userId

*/
delete from MBThreadFlag where threadFlagId in (49172, 49173, 82010, 82011, 82012, 82014, 82016, 82017, 83760, 83759, 87768, 87767, 171631, 178538);

/* removed user & group */
update MBMessage set userId = 10485 where userId = 20538;
update MBThread set rootMessageUserId = 10485 where rootMessageUserId = 20538;
update MBThread set lastPostByUserId = 10485 where lastPostByUserId = 20538;

update MBMessage set userId = 12380 where userId = 33404;
update MBThread set rootMessageUserId = 12380 where rootMessageUserId = 33404;
update MBThread set lastPostByUserId = 12380 where lastPostByUserId = 33404;

delete from MBMessage where groupId = 16527;
delete from MBThread where groupId = 16527;

/* migration problems */
delete from ResourceBlock where resourceBlockId in (1, 3);

/* reason: user was deleted in db */
update AssetEntry set userId = 10485 where userId = 20538;
update AssetEntry set userId = 12380 where userId = 33404;

/* deleted group */
delete from AssetEntry where groupId = 16527;

/* to omit the SocialActivity Update Bug https://issues.liferay.com/browse/LPS-44751
Identify bad entries
====================
select a.groupId, a.userId, a.modifiedDate, a.resourcePrimKey, a.userName, count(*) from WikiPage a 
        inner join WikiPage b on (a.groupId = b.groupId and
          a.userId = b.userId and a.modifiedDate = b.modifiedDate 
          and a.resourcePrimKey = b.resourcePrimKey)
        group by a.groupId, a.userId, a.modifiedDate, a.resourcePrimKey, a.userName
        
select a.pageId, modifiedDate from WikiPage a 
        where a.resourcePrimKey in ( 115708, 115708, 115715)
*/
UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 11:59:09') WHERE pageId = 115707;
UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 11:59:08') WHERE pageId = 115740;

UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 12:00:01') WHERE pageId = 115722;
UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 12:00:02') WHERE pageId = 115744;

UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 12:01:15') WHERE pageId = 115714;
UPDATE WikiPage SET modifiedDate = TIMESTAMP('2013-05-10 12:01:16') WHERE pageId = 115742;

/* mainly not used */
delete from Chat_Status;
delete from Chat_Entry;

/* migrate bookmarks */ 
SET sql_mode='ANSI_QUOTES';

CREATE TABLE BookmarksEntry_tmp ("uuid_" varchar(75), entryId bigint NOT NULL, groupId bigint, companyId bigint, userId bigint, userName varchar(75), createDate datetime, modifiedDate datetime, resourceBlockId bigint, folderId bigint, name varchar(255), url longtext, description longtext, visits int, priority int, PRIMARY KEY (entryId)) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into BookmarksEntry_tmp
        select bm."uuid_", bm.entryId, bm.groupId, bm.companyId, bm.userId, bm.userName, 
          bm.createDate, bm.modifiedDate, bm.resourceBlockId, bm.folderId, 
          bm.name, bm.url, eval."data_" as description, bm.visits, bm.priority
        from ExpandoValue eval
          join BookmarksEntry bm on (bm.entryId = eval.classPK)
        where eval.tableId = 11837;

DROP TABLE BookmarksEntry;  

CREATE TABLE `BookmarksEntry` (`uuid_` varchar(75), `entryId` bigint NOT NULL, `groupId` bigint, `companyId` bigint, `userId` bigint, `userName` varchar(75), `createDate` datetime, `modifiedDate` datetime, `resourceBlockId` bigint, `folderId` bigint, `name` varchar(255), `url` longtext, description longtext, `visits` int, `priority` int, PRIMARY KEY (`entryId`), CONSTRAINT `IX_EAA02A91` UNIQUE (`uuid_`, `groupId`), INDEX `IX_E52FF7EF` (`groupId`), INDEX `IX_5200100C` (`groupId`, `folderId`), INDEX `IX_E2E9F129` (`groupId`, `userId`), INDEX `IX_B670BA39` (`uuid_`), INDEX `IX_E848278F` (`resourceBlockId`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into BookmarksEntry
  select * from BookmarksEntry_tmp;
  
DROP TABLE BookmarksEntry_tmp;
  
/* set new default theme */
update LayoutSet set themeId = 'politaktivdefault_WAR_politaktivdefaulttheme', wapThemeId = 'politaktivdefault_WAR_politaktivdefaulttheme';