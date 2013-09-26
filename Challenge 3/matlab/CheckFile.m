function [status, oldLen, newTrace]= CheckFile(fileName, oldLen)

% status == 0 means ok;
% status == 1 means file open error
% status == 2 means no update
% status == 3 means first time to check this file.
status = 0;
newTrace = [];

fid = fopen(fileName);
if fid == -1
    status = 1;
    newTrace = [];
    return;
end

pos = 0;

% Get the length of current file
fseek(fid, 0, 'eof');
fLen = ftell(fid);
% fLen,
if fLen < oldLen
    error('File Size reduced!')
elseif fLen == oldLen
 %   disp('Ther is no update about data file.')
 %   pause(period);
    status = 2;
    fclose(fid);
    return;
end
if oldLen == 0
    oldLen = fLen;
    status = 3;
    fclose(fid);
    return;
end

% Move to the head of new content
fseek(fid, oldLen, 'bof');
oldLen = fLen;

% The first part of res.txt is useless, so we distinguish the case when
% it is the first update of res.txt or not. Write like this just for acclerating the
% program.

%if oldLen == 0
while ~feof(fid)
    tline = fgetl(fid);
    disp(tline);
    
    % ATTENTION, THIS LINE DEPENDS ON THE MESSAGE FORMAT YOU DEFINE
    if (length(tline) < 20 ) || (~strcmp(tline(13:20), 'SunSPOT:'))
        continue;
    end
    %tline
    info = ParseLine(tline);
    %trace =  [trace, info]; % Very Slow, Revise later
    newTrace = [newTrace, info];
  %  newTrace,
end

fclose(fid);

if isempty(newTrace)
    status = 2;
end