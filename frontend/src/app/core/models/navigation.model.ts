import { UserScope } from './auth.model';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  section: 'core' | 'clinical' | 'operations' | 'administration';
  phaseTag?: string;
  requiredPermissions?: readonly string[];
  requiredScope?: UserScope;
}

export interface BreadcrumbItem {
  label: string;
  route: string;
}
