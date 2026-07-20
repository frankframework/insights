import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { FrankFrameworkMemberGuard } from './frankframework-member.guard';
import { AuthService, User } from '../services/auth.service';

const runGuard = () => TestBed.runInInjectionContext(() => FrankFrameworkMemberGuard({} as any, {} as any));

describe('FrankFrameworkMemberGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://example.com/avatar.jpg',
    isFrankFrameworkMember: true,
  };

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['checkAuthStatus', 'setAuthenticated'], {
      currentUser: signal<User | null>(null),
    });
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should allow access when user is a FrankFramework member', () => {
    authService.currentUser.set(mockUser);

    const result = runGuard();

    expect(result).toBe(true);
    expect(authService.checkAuthStatus).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and redirect when user is not a FrankFramework member', () => {
    authService.currentUser.set({ ...mockUser, isFrankFrameworkMember: false });

    const result = runGuard();

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should resolve auth on a cold load and allow a member (e.g. opened in a new tab)', (done) => {
    authService.currentUser.set(null);
    authService.checkAuthStatus.and.returnValue(of(mockUser));

    (runGuard() as Observable<boolean>).subscribe((allowed) => {
      expect(allowed).toBe(true);
      expect(authService.setAuthenticated).toHaveBeenCalledWith(mockUser);
      expect(router.navigate).not.toHaveBeenCalled();
      done();
    });
  });

  it('should resolve auth on a cold load and redirect a non-member', (done) => {
    authService.currentUser.set(null);
    authService.checkAuthStatus.and.returnValue(of({ ...mockUser, isFrankFrameworkMember: false }));

    (runGuard() as Observable<boolean>).subscribe((allowed) => {
      expect(allowed).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      done();
    });
  });

  it('should redirect when there is no authenticated user', (done) => {
    authService.currentUser.set(null);
    authService.checkAuthStatus.and.returnValue(of(null));

    (runGuard() as Observable<boolean>).subscribe((allowed) => {
      expect(allowed).toBe(false);
      expect(authService.setAuthenticated).not.toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      done();
    });
  });
});
