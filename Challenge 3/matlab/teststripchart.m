x = 1:1000;
y = sin(2*pi*x/1000);
    hLine = plot(x,y);
     StripChart('Initialize',gca)
     for i=1:1000
       StripChart('Update',hLine,y(i))
     end