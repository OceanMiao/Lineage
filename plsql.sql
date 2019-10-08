/*
truncate table MAG_ANALYTICS_00;
truncate table MAG_ANALYTICS_01;
truncate table MAG_ANALYTICS_02;
truncate table MAG_ANALYTICS_03;
truncate table MAG_ANALYTICS_04;
truncate table MAG_ANALYTICS_05;
truncate table MAG_ANALYTICS_06;
truncate table MAG_ANALYTICS_07;
truncate table MAG_ANALYTICS_08;
truncate table MAG_ANALYTICS_09;
truncate table MAG_ANALYTICS_10;
truncate table MAG_ANALYTICS_11;

truncate table MAG_LOG_STAG1;
*/




/************************************************************/
/*************************************************************/
/*************  OCCHIO AI DUPLICATI**************************/
/*///////////////////////////////****************************/
/***************************************************************/


create or replace procedure MAG_STAG1_DOWNLOAD(dal int, al int)

as


  cella_nott_month_input varchar2(6);

 

-------[Cursore per scorrere i 5 shop]------------------
cursor list_of_shopid is 
	select shop_id as id 
	from MAG_M_SHOPID
  where --shop_id = 88888
	shop_id in (33333,66666,77777,88888,99999)
	order by shop_id;
    


  begin 
    
    FOR current_shopid in list_of_shopid LOOP
    
   declare  
    -------------[Cursore per scorrere tutti i giorni del periodo scelto]----------------------
	cursor list_of_days is 
		SELECT distinct a.day 
		from MAG_DT_TIMESTAMP a
		LEFT JOIN (select * from mag_log_stag1 where shop_id = current_shopid.id and commenti = 'Fine Staging1') b ON a.day = b.day_processed 
		WHERE b.day_processed IS NULL and day between dal and al
		ORDER BY 1;
    
    begin

      FOR current_day in list_of_days LOOP
  
  
  
----------[Recupero mese cella notturna da usare per la BG_MONTH]------------------
    select 
      case 
        when  CELLA_NOTTURNA_MONTH  > substr(current_day.day, 5,2) 
          then (to_number(substr(current_day.day, 1,4)) -1 ) || CELLA_NOTTURNA_MONTH  
          else substr(current_day.day, 1,4) || CELLA_NOTTURNA_MONTH 
      end into cella_nott_month_input
    from m_shop_month where CURRENT_MONTH=substr(current_day.day, 5,2);
  
 
 
 
 
---------[Scaricare dati PS celle interne]---------------
  --drop table MAG_ANALYTICS_00 purge;
  --create table MAG_ANALYTICS_00 nologging compress for query high as
  execute immediate 'truncate table MAG_ANALYTICS_00';
  insert /*+ append */ into  MAG_ANALYTICS_00
    select 
       max(day) day,
       max(shop_id) shop_id,
       starttime,
       imsi,
       max(imei) imei,
       max(is_internal_cell) is_internal_cell,
       sum(n_event_4g) n_event_4g,
       sum(n_event_3g) n_event_3g,
       max(n_event_femto) n_event_femto
    from
    (
        SELECT 
            current_day.day DAY,
            current_shopid.id shop_id,
            STARTTIME,
            IMSI,
            imei,
            'Y' IS_INTERNAL_CELL,
            CASE WHEN TECNOLOGIA ='LTE' THEN n_event ELSE 0 END N_EVENT_4G,
            CASE WHEN TECNOLOGIA='UMTS' THEN n_event ELSE 0 END N_EVENT_3G,
            0 N_EVENT_FEMTO
          FROM report_ps.F_ACCESS_PART_minute a
              JOIN 
                (SELECT cella, TECNOLOGIA  FROM MAG_DT_CELLE_COPERTURA WHERE SHOP_ID IN (current_shopid.id)) B 
              ON a.zci=b.cella    
          WHERE a.STARTTIME BETWEEN to_date(current_day.day || ' 00:00:00','yyyymmdd hh24:mi:ss') AND to_date(current_day.day || ' 23:59:59','yyyymmdd hh24:mi:ss')
          AND (substr(a.IMSI,1,5) IN ('22210') OR SUBSTR(A.IMSI,1,3) <>'222') --ITALIANI O ROAMERS --ITALIANI O ROAMERS
          ) 
      group by imsi,starttime;

      commit;
      
      
      
      
    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_00'
      FROM dual;
    commit;
    
    
 
 
--------------[Scaricare dati Femto + unire alla tabella dei dati interni PS]------------------  
    --drop table MAG_ANALYTICS_01 purge;
    --create table MAG_ANALYTICS_01 compress for query high as
    execute immediate 'truncate table MAG_ANALYTICS_01';
    insert /*+ append */ into  MAG_ANALYTICS_01
      select 
          current_day.day day,
          current_shopid.id shop_id,
          A.ENTERDATE STARTTIME,
          a.imsi imsi,
          b.imei_model imei,
          'Y' IS_INTERNAL_CELL,
          0 N_EVENT_4G,
          0 N_EVENT_3G,
          1 N_EVENT_FEMTO
      from big_shop.PRESENCE_LOG_shop_id a 
            LEFT JOIN 
            (select IMSI, MAX(IMEI_MODEL) IMEI_MODEL from REPORT_PS.ULISSE_BG_MTH where month=cella_nott_month_input group by imsi) B   --per recuperare imei 
            ON A.IMSI=B.IMSI
      WHERE to_number(to_char(to_date(A.DAY,'yyyy-MM-dd'),'yyyymmdd')) =current_day.day
             AND SUBSTR(SHOP_ID,1,5) IN (current_shopid.id) AND  (substr(a.IMSI,1,5) IN ('22210') OR SUBSTR(A.IMSI,1,3) <>'222')
  
  UNION ALL
  
      select
          day,
          shop_id,
          starttime,
          imsi,
          imei,
          IS_INTERNAL_CELL,
          n_event_4g,
          n_event_3g,
          n_event_femto
      from MAG_ANALYTICS_00
        

  commit;
    
    

    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_01'
      FROM dual;
    commit;
    
    
 
 

    
-------[Individuo l'IMEI più utilizzato per ogni IMSI]------
    --drop table MAG_ANALYTICS_02 purge;
    --create table MAG_ANALYTICS_02 compress for query high nologging as
    execute immediate 'truncate table MAG_ANALYTICS_02';
    insert /*+ append */ into MAG_ANALYTICS_02
       select        
             a.day,
             a.shop_id,
             a.starttime,
             a.imsi,
             b.imei,
             a.is_internal_cell,
             a.n_event_4G,
             a.n_event_3g,
             a.n_event_femto
        from MAG_ANALYTICS_01 a left join (select imsi , imei 
                                            from (
                                                  select day, imsi, imei, row_number() over (partition by imsi order by N_EVENT desc) as numero
                                                  from (
                                                        select day, imsi, imei, sum(n_event_3g + n_event_4g + n_event_femto) as N_EVENT
                                                        from MAG_ANALYTICS_01
                                                        group by day, imsi, imei
                                                        ) a
                                                  )
                                              where numero = 1
                                             ) b 
                                      on a.imsi = b.imsi;
                                           
commit;



    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_02'
      FROM dual;
    commit;
    



-------------[Estraggo i dati da ps per le celle esterne]-------------
    --drop table MAG_ANALYTICS_03 purge;
    --create table MAG_ANALYTICS_03 compress for query high nologging as
    execute immediate 'truncate table MAG_ANALYTICS_03';
    insert /*+ append */ into MAG_ANALYTICS_03
        select 
            max(day) day,
            max(shop_id) shop_id,
            starttime,
            imsi,
            max(imei) imei,
            max(is_internal_cell) is_internal_cell,
            max(N_EVENT_4G) N_EVENT_4G,
            max(N_EVENT_3G) N_EVENT_3G,
            max(N_EVENT_FEMTO) N_EVENT_FEMTO
        from
        (
            SELECT 
                  current_day.day day,
                  current_shopid.id shop_id,
                  a.STARTTIME,
                  a.IMSI,
                  a.imei,
                  'N' IS_INTERNAL_CELL,
                   CAST(NULL AS NUMBER) N_EVENT_4G,
                   CAST(NULL AS NUMBER) N_EVENT_3G,
                   CAST(NULL AS NUMBER) N_EVENT_FEMTO
              FROM report_ps.F_ACCESS_PART_minute a join MAG_ANALYTICS_02 c on a.imsi=c.imsi
              WHERE a.STARTTIME BETWEEN to_date(current_day.day || ' 00:00:00','yyyymmdd hh24:mi:ss') AND to_date(current_day.day || ' 23:59:59','yyyymmdd hh24:mi:ss')
              and a.zci not in (
                  SELECT  cella
                  FROM MAG_DT_CELLE_COPERTURA where shop_id IN (current_shopid.id)
                )
          ) 
        group by imsi, starttime; 

commit;



    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_03'
      FROM dual;
    commit;




----------[Creao un'unica tabella con celle interne e celle esterne]------------
    --drop table MAG_ANALYTICS_04 purge;
    --create table MAG_ANALYTICS_04 compress for query high nologging as--
    execute immediate 'truncate table MAG_ANALYTICS_04';
    insert /*+ append */ into MAG_ANALYTICS_04
        select * from MAG_ANALYTICS_02 --celle interne
      union all
        select * from MAG_ANALYTICS_03; -- celle esterne

    commit;



    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_04'
      FROM dual;
    commit;  
    




-------[Etichetto le entrate e le uscite]---------
    --drop table MAG_ANALYTICS_05 purge;
    --create table MAG_ANALYTICS_05 compress for query high nologging as
    execute immediate 'truncate table MAG_ANALYTICS_05';
    insert /*+ append */ into MAG_ANALYTICS_05
         SELECT 
            a.day,
            SHOP_ID,
            STARTTIME,
            IMSI,
            imei,
            IS_INTERNAL_CELL,
            N_EVENT_4G,
            N_EVENT_3G,
            N_EVENT_FEMTO,
            prev_is_internal_cell,
            CASE WHEN PREV_IS_INTERNAL_CELL is null and IS_INTERNAL_CELL ='Y'  THEN 'START'
                 WHEN PREV_IS_INTERNAL_CELL = 'N' AND IS_INTERNAL_CELL ='Y' THEN 'START'
                 when next_is_internal_cell='N' and is_internal_cell='Y' then 'END'
                 when next_is_internal_cell is null and is_internal_cell = 'Y' then 'END' 
                 ELSE NULL
              END START_END  
          FROM
          (
              SELECT 
                  DAY,
                  SHOP_ID,
                  STARTTIME,
                  IMSI,
                  imei,
                  IS_INTERNAL_CELL,
                  N_EVENT_4G,
                  N_EVENT_3G,
                  N_EVENT_FEMTO,
                  LAG(is_internal_cell) OVER (PARTITION BY imsi ORDER BY starttime) AS prev_is_internal_cell,
                  lead(is_internal_cell) over (PARTITION BY imsi ORDER BY starttime) AS next_is_internal_cell
               FROM MAG_ANALYTICS_04
            ) A;

    commit;
    


    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_05'
      FROM dual;
    commit;  





----------[Mantengo solo le righe che mi permettono di definire inizio e fine della visita allo shop]----------------
    --drop table MAG_ANALYTICS_06 purge;
    --create table MAG_ANALYTICS_06 compress for query high nologging as
    execute immediate 'truncate table MAG_ANALYTICS_06';
    insert /*+ append */ into MAG_ANALYTICS_06
      SELECT * 
      FROM MAG_ANALYTICS_05
      WHERE START_END IS NOT NULL;
    
    commit;




    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_06'
      FROM dual;
    commit; 





------------[Creao una tabella in cui metto in riga l'orario di entrata e uscita per ciascun utente]----------------
    --drop table MAG_ANALYTICS_07 purge;
    --create table MAG_ANALYTICS_07 compress for query high nologging as
    execute immediate 'truncate table MAG_ANALYTICS_07';
    insert /*+ append */ into MAG_ANALYTICS_07
      SELECT 
          DAY,
          shop_id,
          IMSI,
          IMEI,
          ENTRANCETIMESTAMP,
          EXITTIMESTAMP,
          extract(hour from EXITTIMESTAMP-ENTRANCETIMESTAMP)*60 + extract(minute from EXITTIMESTAMP-ENTRANCETIMESTAMP) dwelltime_minutes
      FROM
      (
        SELECT  DAY,shop_id,STARTTIME,IMSI, imei, 
                  CASE WHEN START_END ='START' THEN STARTTIME ELSE NULL END AS ENTRANCETIMESTAMP,
                  CASE WHEN START_END ='START' THEN NEXT_STARTTIME ELSE NULL END AS EXITTIMESTAMP
        FROM
        (
            SELECT  A.*,
              LEAD(STARTTIME) OVER (PARTITION BY imsi, shop_id ORDER BY starttime) AS NEXT_STARTTIME
            FROM MAG_ANALYTICS_06  A
        ) A
      )
      WHERE ENTRANCETIMESTAMP IS NOT NULL;

    commit;
   
    


    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_07'
      FROM dual;
    commit; 





-----------[raggruppo righe relative ad uno stesso utente per cui la differenza di entrance timestamp e exittimestamp è minore di 30 minuti li unisco]--------------------------------
    --drop table MAG_ANALYTICS_08 purge;
    --create table MAG_ANALYTICS_08 compress for query high as
    execute immediate 'truncate table MAG_ANALYTICS_08';
    insert /*+ append */ into MAG_ANALYTICS_08
      SELECT 
          DAY,
          SHOP_ID,
          IMSI,
          IMEI,
          ENTRANCETIMESTAMP,
          EXITTIMESTAMP,
          extract(hour from EXITTIMESTAMP-ENTRANCETIMESTAMP)*60 + extract(minute from EXITTIMESTAMP-ENTRANCETIMESTAMP) dwelltime_minutes
      from
      (
          select 
              day,
              shop_id,
              imsi,
              imei,
              entrancetimestamp,
              FLAG_MERGE_START_STOP,
             /* CASE
                WHEN FLAG_MERGE_START_STOP = 'START' and (LEAD(FLAG_MERGE_START_STOP) over (PARTITION BY imsi ORDER BY entrancetimestamp)) = 'STOP' THEN LEAD(exittimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp)
                ELSE exittimestamp
              END exittimestamp */
             CASE when FLAG_MERGE_START_STOP = 'START'  and ((LEAD(FLAG_MERGE_START_STOP) over (PARTITION BY imsi ORDER BY entrancetimestamp) is null) or (LEAD(FLAG_MERGE_START_STOP) over (PARTITION BY imsi ORDER BY entrancetimestamp) = 'START')) then  exittimestamp
              when FLAG_MERGE_START_STOP = 'START'  and LEAD(FLAG_MERGE_START_STOP) over (PARTITION BY imsi ORDER BY entrancetimestamp) = 'STOP' and  LEAD(exittimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp) is not null then  LEAD(exittimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp)
              when FLAG_MERGE_START_STOP = 'START'  and LEAD(FLAG_MERGE_START_STOP) over (PARTITION BY imsi ORDER BY entrancetimestamp) = 'STOP' and  LEAD(exittimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp) is  null then  LEAD(entrancetimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp)
              else null
              end exittimestamp
          FROM
            (
                select 
                    A.*,
                    CASE
                      WHEN PREV_exittimestamp is null THEN 'START'
                      WHEN (extract(hour from entrancetimestamp-PREV_exittimestamp)*60 + extract(minute from entrancetimestamp-PREV_exittimestamp))>30 THEN 'START'
                      WHEN (extract(hour from NEXT_entrancetimestamp-exittimestamp)*60 + extract(minute from NEXT_entrancetimestamp-exittimestamp))>30 or next_entrancetimestamp is null THEN 'STOP'
                      ELSE NULL
                    END FLAG_MERGE_START_STOP
                from
                (
                  select 
                    a.*,
                    LAG(exittimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp) as PREV_exittimestamp,
                    LEAD(entrancetimestamp) over (PARTITION BY imsi ORDER BY entrancetimestamp) as NEXT_entrancetimestamp
                  from MAG_ANALYTICS_07 a
                ) A 
              ) a WHERE FLAG_MERGE_START_STOP is not null
          ) a
      where a.FLAG_MERGE_START_STOP = 'START';
  
      commit;
  


    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_08'
      FROM dual;
    commit; 


 
 
  


----------[Calcolo gli eventi per ogni starttime endtime]-------------
    --drop table MAG_ANALYTICS_09 purge;
    --create table MAG_ANALYTICS_09 compress for query high nologging as
    delete from MAG_ANALYTICS_09 where day=current_day.day and shop_id = current_shopid.id ; commit;    --Manteniamo lo storico pre-filtri M2M e Virtual Operator
    insert into MAG_ANALYTICS_09 
    select  
          max(b.day) day, 
          max(b.shop_id) shop_id, 
          a.imsi,
          max(b.imei) imei,
          b.entrancetimestamp,
          b.exittimestamp,
          max(b.dwelltime_minutes) dwelltime_minutes,
          sum(nvl(a.n_event_4g,0)) n_event_4g, 
          sum(nvl(a.n_event_3g,0)) n_event_3g,
          sum(nvl(a.n_event_femto,0)) n_event_femto
      from MAG_ANALYTICS_05 a JOIN MAG_ANALYTICS_08 b 
              ON a.imsi=b.imsi and ((a.starttime >= b.entrancetimestamp and a.starttime<=b.exittimestamp) or (a.starttime >= b.entrancetimestamp and b.exittimestamp is null)) --gestione dell'exit null
      where a.is_internal_cell='Y' --considero solo gli eventi generati in celle interne
      group by a.imsi,b.entrancetimestamp, b.exittimestamp;

    commit;



    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_09'
      FROM dual;
    commit; 




--[Filtri per escludere modem e operatori virtuali]--
  --drop table MAG_ANALYTICS_10purge;
  --create table MAG_ANALYTICS_10 compress for query high nologging as
  delete from MAG_ANALYTICS_10 where day=current_day.day and shop_id = current_shopid.id ; commit;      ----> Manteniamo lo storico pre-filtro orari chiusura (dwel time > xx?)
  insert into MAG_ANALYTICS_10 
      SELECT *
      FROM MAG_ANALYTICS_09 a
      where day=current_day.day and shop_id = current_shopid.id  
                                                and NOT EXISTS (
                                                  select tac 
                                                  from (
                                                          SELECT tac
                                                          FROM analysis.modem
                                                          where provenienza <> 'E2652'
                                                                  ) e
                                                  WHERE e.TAC=substr(a.imei,1,8)
                                                )
            and substr(a.imsi,1,7) not in ('2221013','2221014');

commit;




    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Aggiornata tabella MAG_ANALYTICS_10'
      FROM dual;
    commit; 





----------[Filtro gli utenti in base alla permanenza , eventi FEMTO , entrata e uscita dallo shop  ----> TABELLA DI FINE STAGING1]-------------------
    --drop table MAG_ANALYTICS_11 purge;
    --create table MAG_ANALYTICS_11 compress for query high nologging as
    delete from MAG_ANALYTICS_11 where day=current_day.day and shop_id =current_shopid.id ; commit; 
    insert into MAG_ANALYTICS_11
      select a.*
      from MAG_ANALYTICS_10 a left join MAG_M_ORARI_SHOP b on a.shop_id = b.shop_id and a.day = b.day
      where  substr(to_char(entrancetimestamp),11,2) || substr(to_char(entrancetimestamp),14,2) >= b.enter_bound  and 
             substr(to_char(entrancetimestamp),11,2) || substr(to_char(entrancetimestamp),14,2) <= b.close_hour  and
             substr(to_char(exittimestamp),11,2) || substr(to_char(exittimestamp),14,2) <= b.exit_bound
             AND A.DAY=CURRENT_DAY.DAY
             and a.shop_id = current_shopid.id;
             
             
    commit;

    

    
    INSERT INTO MAG_LOG_STAG1 
      SELECT current_day.day, current_timestamp ,  current_shopid.id , 'Fine Staging1'
      FROM dual;
    commit;
      
	
	

	

		end loop;
    end;--fine loop giorno
	end loop;  --fine loop shop 
end MAG_STAG1_DOWNLOAD;


--26 minuti a giorno e shop


-----> CI SONO 127.710.434 righe nella tabella mag_analytics_11