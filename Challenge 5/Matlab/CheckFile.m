
function [status, oldLen, newTrace]= CheckFile(fileName, oldLen)

status = 0;
% status == 0 means ok;
% status == 1 means file open error
% status == 2 means no update
% status == 3 means first time to check this file.
newTrace = [];
period = 1/5;

% Check if file opened correctly
fid = fopen(fileName);
if fid == -1
    status = 1;
    return;
end

% Get the length of current file
fseek(fid, 0, 'eof');
fLen = ftell(fid);

if fLen < oldLen
    error('File Size reduced!')
elseif fLen == oldLen
    pause(period);
    status = 2;
    fclose(fid);
    return;
end

% Move to the head of new content
fseek(fid, oldLen, 'bof');
oldLen = fLen;

while ~feof(fid)
    tline = fgetl(fid);
    % Check if most recent line in file is not empty
    if (length(tline) < 10 )
        continue;
    end
    info = ParseLine(tline);
    % disp('Data: ');
    % disp(info);
    newTrace = [newTrace, info];
end

fclose(fid);
if isempty(newTrace)
    status = 2;
end