import {
  createIcons,AlignCenter,AlignJustify,AlignLeft,AlignRight,ArrowLeft,Bookmark,
  CalendarCheck,CalendarDays,CaseUpper,Check,ChevronLeft,ChevronRight,Circle,
  Columns2,Columns3,Eye,Flag,Languages,LayoutTemplate,List,ListFilter,ListTodo,
  Maximize,Minus,Monitor,Moon,Newspaper,Palette,PanelsTopLeft,Plus,
  RectangleHorizontal,RectangleVertical,RotateCcw,RotateCw,Rows3,Scan,Settings,
  SlidersHorizontal,Sprout,Square,Sun,Type,Underline,X,
} from 'lucide';

const icons={AlignCenter,AlignJustify,AlignLeft,AlignRight,ArrowLeft,Bookmark,
  CalendarCheck,CalendarDays,CaseUpper,Check,ChevronLeft,ChevronRight,Circle,
  Columns2,Columns3,Eye,Flag,Languages,LayoutTemplate,List,ListFilter,ListTodo,
  Maximize,Minus,Monitor,Moon,Newspaper,Palette,PanelsTopLeft,Plus,
  RectangleHorizontal,RectangleVertical,RotateCcw,RotateCw,Rows3,Scan,Settings,
  SlidersHorizontal,Sprout,Square,Sun,Type,Underline,X};
export function renderIcons(root){createIcons({icons,root,attrs:{width:20,height:20}});}
